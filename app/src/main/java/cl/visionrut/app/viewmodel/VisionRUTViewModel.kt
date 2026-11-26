package cl.visionrut.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.visionrut.app.data.ReglaIncumplida
import cl.visionrut.app.data.Categoria
import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.Egreso
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.data.RoomVisionRUTRepository
import cl.visionrut.app.data.VisionDatabase
import cl.visionrut.app.data.VisionRUTData
import cl.visionrut.app.data.VisionRUTRepository
import cl.visionrut.app.domain.CategoryTotal
import cl.visionrut.app.domain.Suscripcion
import cl.visionrut.app.domain.agruparSuscripciones
import cl.visionrut.app.domain.Projection
import cl.visionrut.app.domain.CalculadorResumen.distribution
import cl.visionrut.app.domain.CalculadorResumen.expensesInMonth
import cl.visionrut.app.domain.CalculadorResumen.projection
import cl.visionrut.app.notifications.ProgramadorRecordatoriosAndroid
import cl.visionrut.app.notifications.ProgramadorRecordatorios
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado inmutable que las pantallas observan y representan. */
data class VisionRUTUiState(
    val categories: List<Categoria> = emptyList(),
    val expenses: List<Egreso> = emptyList(),
    val reminders: List<Recordatorio> = emptyList(),
    val suscripciones: List<Suscripcion> = emptyList(),
    val months: Set<String> = emptySet(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val selectedMonth: YearMonth = DemoData.mesDeReferencia(),
    val categoryFilter: String = "all",
    val isSelecting: Boolean = false,
    val selectedExpenseIds: Set<Long> = emptySet(),
    val monthlyExpenses: List<Egreso> = emptyList(),
    val filteredExpenses: List<Egreso> = emptyList(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val forecast: Projection = Projection(),
)

/** Gestiona el estado de VisionRUT y solicita sus operaciones al repositorio. */
class VisionRUTViewModel(
    private val repository: VisionRUTRepository,
    private val notifications: ProgramadorRecordatorios,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    // Estado de carga, errores y filtros restaurables.
    private val loaded = MutableStateFlow(false)
    private val failure = MutableStateFlow<String?>(null)
    private val controls = MutableStateFlow(readControls())
    private val refresh = MutableStateFlow(0)
    private val errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val errors = errorEvents.asSharedFlow()

    // Reintentar vuelve a suscribirse al repositorio después de un error de lectura.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val repositoryData = refresh.flatMapLatest {
        repository.observeData().catch { error ->
            if (error is CancellationException) throw error
            failure.value = "No pudimos leer los datos. Intenta nuevamente."
            loaded.value = true
            emit(VisionRUTData())
        }
    }

    val uiState =
        combine(repositoryData, controls, loaded, failure) { data, options, ready, error ->
                createUiState(data, options, ready, error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VisionRUTUiState())

    init {
        reload()
    }

    fun reload() = execute {
        loaded.value = false
        repository.initialize()
        notifications.restore(repository.getReminders())
        refresh.value += 1
        loaded.value = true
    }

    // Mes, filtro y selección: la interfaz envía acciones en lugar de modificar el estado.
    fun selectMonth(month: YearMonth) {
        if (month > YearMonth.now()) return
        updateControls { copy(month = month, selectedIds = emptySet()) }
    }

    fun selectFilter(filter: String) = updateControls {
        copy(filter = filter, selectedIds = emptySet())
    }

    fun toggleSelectionMode() = updateControls {
        copy(selecting = !selecting, selectedIds = emptySet())
    }

    fun startSelection(id: Long) = updateControls {
        copy(selecting = true, selectedIds = selectedIds + id)
    }

    fun toggleSelectedExpense(id: Long) = updateControls {
        copy(selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id)
    }

    fun cancelSelection() = updateControls {
        copy(selecting = false, selectedIds = emptySet())
    }

    // Edición de egresos y asignación de categorías.
    fun changeExpenses(
        ids: Set<Long>, categoryId: Long?, note: String? = null,
        confirmedReminders: Set<Long> = emptySet(), done: () -> Unit,
    ) = execute {
        val removed = repository.updateExpenses(ids, categoryId, note, confirmedReminders)
        removed.forEach(notifications::cancel)
        if (controls.value.selecting && controls.value.selectedIds == ids) cancelSelection()
        done()
    }

    fun createCategory(name: String, icon: String, color: Long, done: () -> Unit) = execute {
        repository.createCategory(name, icon, color)
        done()
    }

    fun deleteCategory(category: Categoria, done: () -> Unit) = execute {
        repository.deleteCategory(category)
        done()
    }

    // Recordatorios: persistencia mediante Repository y avisos mediante un adaptador.
    fun saveReminder(
        original: Recordatorio?,
        expense: Egreso,
        service: String,
        date: LocalDate,
        frequency: String,
        done: () -> Unit,
    ) = execute {
        val reminder = repository.saveReminder(original, expense, service, date, frequency)
        notifications.schedule(reminder)
        done()
    }

    fun toggle(reminder: Recordatorio, enabled: Boolean) = execute {
        repository.toggleReminder(reminder, enabled)
        notifications.schedule(reminder.copy(enabled = enabled))
    }

    fun deleteReminder(reminder: Recordatorio, done: () -> Unit) = execute {
        repository.deleteReminder(reminder.id).forEach(notifications::cancel)
        done()
    }

    fun restoreReminders() = execute { notifications.restore(repository.getReminders()) }

    fun previewReminder(id: Long, sent: (String) -> Unit) = execute {
        val reminder = repository.getReminder(id) ?: return@execute
        if (!reminder.enabled) return@execute
        if (!notifications.preview(reminder))
            throw ReglaIncumplida("Activa las notificaciones de VisiónRUT para enviar este aviso.")
        sent(reminder.service)
    }

    // Preparación de los datos listos para mostrar.
    private fun createUiState(
        data: VisionRUTData,
        options: Controls,
        ready: Boolean,
        error: String?,
    ): VisionRUTUiState {
        val filter =
            options.filter.takeIf {
                it in setOf("all", "none") || data.categories.any { category -> category.id.toString() == it }
            } ?: "none"
        val monthly = expensesInMonth(data.expenses, options.month)
        val filtered = monthly.filter { expense ->
            when (filter) {
                "all" -> true
                "none" -> expense.categoryId == null
                else -> expense.categoryId == filter.toLongOrNull()
            }
        }
        val subscriptions = agruparSuscripciones(data.expenses, data.reminders)
        return VisionRUTUiState(
            categories = data.categories,
            expenses = data.expenses,
            reminders = subscriptions.mapNotNull { it.aviso }.sortedBy { it.id },
            suscripciones = subscriptions,
            months = data.months,
            loading = !ready,
            errorMessage = error,
            selectedMonth = options.month,
            categoryFilter = filter,
            isSelecting = options.selecting,
            selectedExpenseIds = options.selectedIds.intersect(monthly.map { it.id }.toSet()),
            monthlyExpenses = monthly,
            filteredExpenses = filtered,
            categoryTotals = distribution(monthly, data.categories),
            forecast = projection(data.expenses, data.categories, options.month),
        )
    }

    private fun execute(action: suspend () -> Unit) = viewModelScope.launch {
        failure.value = null
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            loaded.value = true
            val message = (error as? ReglaIncumplida)?.message ?: "No pudimos completar la operación. Intenta nuevamente."
            failure.value = message
            errorEvents.emit(message)
        }
    }

    // SavedStateHandle conserva el estado temporal cuando Android recrea la pantalla.
    private fun readControls() =
        Controls(
            month =
                runCatching { YearMonth.parse(savedState.get<String>(KEY_MONTH)) }
                    .getOrDefault(DemoData.mesDeReferencia()),
            filter = savedState[KEY_FILTER] ?: "all",
            selecting = savedState[KEY_SELECTING] ?: false,
            selectedIds = savedState.get<ArrayList<Long>>(KEY_SELECTED).orEmpty().toSet(),
        )

    private fun updateControls(change: Controls.() -> Controls) {
        val next = controls.value.change()
        controls.value = next
        savedState[KEY_MONTH] = next.month.toString()
        savedState[KEY_FILTER] = next.filter
        savedState[KEY_SELECTING] = next.selecting
        savedState[KEY_SELECTED] = ArrayList(next.selectedIds)
    }

    private data class Controls(
        val month: YearMonth,
        val filter: String,
        val selecting: Boolean,
        val selectedIds: Set<Long>,
    )

    private companion object {
        const val KEY_MONTH = "selected_month"
        const val KEY_FILTER = "category_filter"
        const val KEY_SELECTING = "is_selecting"
        const val KEY_SELECTED = "selected_expenses"
    }
}

/** Construye el ViewModel con sus dependencias y estado restaurable. */
val VisionRUTViewModelFactory = viewModelFactory {
    initializer {
        val application = checkNotNull(this[APPLICATION_KEY])
        VisionRUTViewModel(
            RoomVisionRUTRepository(VisionDatabase.get(application)),
            ProgramadorRecordatoriosAndroid(application),
            createSavedStateHandle(),
        )
    }
}
