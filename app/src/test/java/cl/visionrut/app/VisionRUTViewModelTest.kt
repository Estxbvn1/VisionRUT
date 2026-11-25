package cl.visionrut.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import cl.visionrut.app.data.SUBSCRIPTIONS_ID
import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.IncludedCategories
import cl.visionrut.app.data.Categoria
import cl.visionrut.app.data.Egreso
import cl.visionrut.app.data.Recordatorio
import cl.visionrut.app.data.VisionRUTData
import cl.visionrut.app.data.VisionRUTRepository
import cl.visionrut.app.notifications.ProgramadorRecordatorios
import cl.visionrut.app.viewmodel.VisionRUTViewModel
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisionRUTViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Before
    fun prepareDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun releaseViewModels() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun TestScope.createModel(
        repository: FakeRepository = FakeRepository(),
        notifications: FakeNotifications = FakeNotifications(),
        handle: SavedStateHandle = SavedStateHandle(),
    ): VisionRUTViewModel {
        val model = VisionRUTViewModel(repository, notifications, handle)
        val store = ViewModelStore().also { stores += it }
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = model as T
            }
        ViewModelProvider(store, factory)[VisionRUTViewModel::class.java]
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        advanceUntilIdle()
        return model
    }

    @Test
    fun filtersAndSelectionAreManagedByTheViewModel() = runTest {
        val model = createModel()
        assertFalse(model.uiState.value.loading)
        assertEquals(DemoData.month(DemoData.mesDeReferencia(), DemoData.mesDeReferencia().atEndOfMonth()).sumOf { it.amount }, model.uiState.value.monthlyExpenses.sumOf { it.amount })
        model.selectFilter("none")
        advanceUntilIdle()
        assertEquals(model.uiState.value.monthlyExpenses.filter { it.categoryId == null }, model.uiState.value.filteredExpenses)
        val id = model.uiState.value.filteredExpenses.first().id
        model.startSelection(id)
        advanceUntilIdle()
        assertTrue(model.uiState.value.isSelecting)
        assertEquals(setOf(id), model.uiState.value.selectedExpenseIds)
        model.selectMonth(DemoData.mesDeReferencia().minusMonths(1))
        advanceUntilIdle()
        assertTrue(model.uiState.value.selectedExpenseIds.isEmpty())
        assertEquals(DemoData.month(DemoData.mesDeReferencia().minusMonths(1)).sumOf { it.amount }, model.uiState.value.monthlyExpenses.sumOf { it.amount })
    }

    @Test
    fun filtersAndSelectionCanBeRestoredFromSavedStateHandle() = runTest {
        val repository = FakeRepository()
        val saved = SavedStateHandle()
        val first = createModel(repository, handle = saved)
        first.selectFilter("1")
        advanceUntilIdle()
        val id = first.uiState.value.filteredExpenses.first().id
        first.startSelection(id)
        val restored = createModel(repository, handle = saved)
        assertEquals("1", restored.uiState.value.categoryFilter)
        assertTrue(restored.uiState.value.isSelecting)
        assertEquals(setOf(id), restored.uiState.value.selectedExpenseIds)
    }

    @Test
    fun assigningASelectionUsesTheRepositoryAndKeepsTheTotal() = runTest {
        val repository = FakeRepository()
        val model = createModel(repository)
        model.selectFilter("none")
        advanceUntilIdle()
        val ids = model.uiState.value.filteredExpenses.map { it.id }.toSet()
        ids.forEach(model::startSelection)
        var completed = false
        model.changeExpenses(ids, 7L) { completed = true }
        advanceUntilIdle()
        assertEquals(ids, repository.lastCategorizedIds)
        assertTrue(completed)
        assertFalse(model.uiState.value.isSelecting)
        assertTrue(model.uiState.value.filteredExpenses.isEmpty())
        assertEquals(DemoData.month(DemoData.mesDeReferencia(), DemoData.mesDeReferencia().atEndOfMonth()).sumOf { it.amount }, model.uiState.value.monthlyExpenses.sumOf { it.amount })
    }

    @Test
    fun aLoadingFailureBecomesVisibleAndCanBeRetried() = runTest {
        val repository = FakeRepository().apply { failInitialization = true }
        val model = createModel(repository)
        assertFalse(model.uiState.value.loading)
        assertNotNull(model.uiState.value.errorMessage)
        repository.failInitialization = false
        model.reload()
        advanceUntilIdle()
        assertFalse(model.uiState.value.loading)
        assertNull(model.uiState.value.errorMessage)
        assertEquals(DemoData.month(DemoData.mesDeReferencia(), DemoData.mesDeReferencia().atEndOfMonth()).size, model.uiState.value.monthlyExpenses.size)
    }

    @Test
    fun deletingAReminderCancelsItsNoticeAndKeepsExpenses() = runTest {
        val repository = FakeRepository()
        val notifications = FakeNotifications()
        val model = createModel(repository, notifications)
        val reminder = model.uiState.value.reminders.first()
        val original = repository.data.value.expenses
        model.deleteReminder(reminder) {}
        advanceUntilIdle()
        assertNull(repository.getReminder(reminder.id))
        assertEquals(listOf(reminder.id), notifications.cancelled)
        assertEquals(original, repository.data.value.expenses)
    }

    @Test
    fun aReadFailureCanBeRetriedWithoutRecreatingTheViewModel() = runTest {
        val repository = FakeRepository().apply { failRead = true }
        val model = createModel(repository)
        assertFalse(model.uiState.value.loading)
        assertNotNull(model.uiState.value.errorMessage)
        assertTrue(model.uiState.value.expenses.isEmpty())
        repository.failRead = false
        model.reload()
        advanceUntilIdle()
        assertNull(model.uiState.value.errorMessage)
        assertEquals(DemoData.month(DemoData.mesDeReferencia(), DemoData.mesDeReferencia().atEndOfMonth()).size, model.uiState.value.monthlyExpenses.size)
    }

    @Test
    fun cancellingAnOperationDoesNotBecomeAnApplicationError() = runTest {
        val repository = FakeRepository()
        val model = createModel(repository)
        repository.cancelCategorization = true
        val task =
            model.changeExpenses(setOf(model.uiState.value.monthlyExpenses.first().id), 1L) {}
        advanceUntilIdle()
        assertTrue(task.isCancelled)
        assertNull(model.uiState.value.errorMessage)
    }

    @Test
    fun missingCategoryFallsBackToUncategorizedExpenses() = runTest {
        val model = createModel()
        val monthly = model.uiState.value.monthlyExpenses
        val pendingIds = monthly.filter { it.categoryId == null }.map { it.id }
        assertTrue(pendingIds.isNotEmpty())

        model.selectFilter("9999")
        advanceUntilIdle()

        assertEquals("none", model.uiState.value.categoryFilter)
        assertEquals(pendingIds, model.uiState.value.filteredExpenses.map { it.id })
        assertEquals(monthly, model.uiState.value.monthlyExpenses)
    }

    @Test
    fun manualNoticeUsesTheStoredEnabledStateAndPreservesTheSchedule() = runTest {
        val repository = FakeRepository()
        val notifications = FakeNotifications()
        val model = createModel(repository, notifications)
        val reminder = model.uiState.value.reminders.single()
        val sent = mutableListOf<String>()

        repository.toggleReminder(reminder, false)
        model.previewReminder(reminder.id) { sent += it }
        advanceUntilIdle()
        assertTrue(sent.isEmpty())
        assertTrue(notifications.previews.isEmpty())

        repository.toggleReminder(reminder, true)
        model.previewReminder(reminder.id) { sent += it }
        advanceUntilIdle()

        assertEquals(listOf(reminder.service), sent)
        assertEquals(listOf(reminder), notifications.previews)
        assertEquals(reminder, repository.getReminder(reminder.id))
        assertTrue(notifications.scheduled.isEmpty())
    }

    private class FakeNotifications : ProgramadorRecordatorios {
        val cancelled = mutableListOf<Long>()
        val previews = mutableListOf<Recordatorio>()
        val scheduled = mutableListOf<Recordatorio>()

        override fun schedule(reminder: Recordatorio) {
            scheduled += reminder
        }

        override fun preview(reminder: Recordatorio): Boolean {
            previews += reminder
            return reminder.enabled
        }

        override fun cancel(id: Long) {
            cancelled += id
        }

        override fun restore(reminders: List<Recordatorio>) = Unit
    }

    private class FakeRepository : VisionRUTRepository {
        val data = MutableStateFlow(VisionRUTData())
        var failInitialization = false
        var failRead = false
        var cancelCategorization = false
        var lastCategorizedIds = emptySet<Long>()

        override fun observeData() = flow {
            check(!failRead)
            emitAll(data)
        }

        override suspend fun initialize() {
            check(!failInitialization)
            if (data.value.expenses.isNotEmpty()) return
            val now = DemoData.mesDeReferencia()
            val expenses = (0..3).flatMap { DemoData.month(now.minusMonths(it.toLong()), now.atEndOfMonth()) }
            val subscription = expenses.first { it.categoryId == SUBSCRIPTIONS_ID }
            data.value =
                VisionRUTData(
                    IncludedCategories,
                    expenses,
                    listOf(
                        Recordatorio(
                            1,
                            subscription.id,
                            "Servicio",
                            LocalDate.now().plusDays(4).toEpochDay(),
                            "mensual",
                        )
                    ),
                    (0..3).map { now.minusMonths(it.toLong()).toString() }.toSet(),
                )
        }

        override suspend fun updateExpenses(ids: Set<Long>, categoryId: Long?, note: String?,
            confirmedReminders: Set<Long>): List<Long> {
            if (cancelCategorization) throw CancellationException()
            lastCategorizedIds = ids
            data.value = data.value.copy(expenses = data.value.expenses.map {
                if (it.id in ids) it.copy(categoryId = categoryId, note = note ?: it.note) else it
            })
            return emptyList()
        }

        override suspend fun getReminder(id: Long) = data.value.reminders.find { it.id == id }

        override suspend fun getReminders() = data.value.reminders

        override suspend fun deleteReminder(id: Long) {
            data.value = data.value.copy(reminders = data.value.reminders.filterNot { it.id == id })
        }

        override suspend fun toggleReminder(reminder: Recordatorio, enabled: Boolean) {
            data.value =
                data.value.copy(
                    reminders =
                        data.value.reminders.map {
                            if (it.id == reminder.id) it.copy(enabled = enabled) else it
                        }
                )
        }

        override suspend fun markNotified(id: Long, due: Long) = Unit

        override suspend fun createCategory(
            name: String,
            icon: String,
            color: Long,
        ) = error("Operación fuera de este caso de prueba")

        override suspend fun deleteCategory(category: Categoria) =
            error("Operación fuera de este caso de prueba")

        override suspend fun saveReminder(
            original: Recordatorio?,
            expense: Egreso,
            service: String,
            date: LocalDate,
            frequency: String,
        ): Recordatorio = error("Operación fuera de este caso de prueba")
    }
}
