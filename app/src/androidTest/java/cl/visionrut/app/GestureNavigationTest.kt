package cl.visionrut.app

import android.Manifest
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.VisionDatabase
import cl.visionrut.app.domain.CalculadorResumen.expensesInMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GestureNavigationTest {
    @get:Rule(order = 0) val database = DemoDatabaseRule()
    @get:Rule(order = 1)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(value: String) =
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }

    private fun title(value: String) {
        compose.onNodeWithTag("screen-title").assertTextEquals(value)
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun enter() {
        waitText("VisionRUT")
        compose.onNodeWithTag("bank-visionrut").performScrollTo().performClick()
        title("Resumen")
    }

    private fun allExpenses() {
        compose.onNodeWithTag("nav-1").performClick()
        compose.onNodeWithTag("filter-picker").performScrollTo().performClick()
        compose.onNodeWithTag("filter-choice-all").performClick()
    }

    private fun expense(id: Long): SemanticsNodeInteraction {
        compose.onNodeWithTag("expense-list").performScrollToKey(id)
        return compose.onNodeWithTag("expense-$id")
    }

    private fun swipeLeft(id: Long) {
        val node = expense(id)
        val size = node.fetchSemanticsNode().size
        node.performTouchInput {
            swipe(
                Offset(size.width * .82f, size.height * .5f),
                Offset(size.width * .08f, size.height * .5f),
                durationMillis = 450,
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun systemBackReturnsToSummaryWhileTheHeaderLeavesTheModuleAndPanelsClose() = runBlocking {
        waitText("VisionRUT")
        compose.onNodeWithContentDescription("Mostrar saldo").performClick()
        compose.onNodeWithTag("bank-balance").assertTextEquals("$999.999.999.999")
        compose.onNodeWithContentDescription("Ocultar saldo").performClick()
        compose.onNodeWithTag("bank-balance").assertTextEquals("$*********")
        enter()
        compose.onNodeWithTag("nav-1").performClick()
        compose.onNodeWithTag("nav-2").performClick()
        back()
        title("Resumen")
        compose.onNodeWithTag("nav-1").performClick()
        back()
        title("Resumen")
        compose
            .onNodeWithTag("summary-list")
            .performScrollToNode(hasTestTag("action-Gestionar categorías"))
        compose.onNodeWithTag("action-Gestionar categorías").performClick()
        compose.onNodeWithText("Crear").performClick()
        back()
        compose.onNodeWithContentDescription("Cerrar panel").assertDoesNotExist()
        title("Resumen")
        allExpenses()
        val dao = VisionDatabase.get(compose.activity).dao()
        val row = expensesInMonth(dao.expenses().first(), DemoData.mesDeReferencia()).first()
        expense(row.id).performClick()
        back()
        title("Egresos")
        compose.onNodeWithContentDescription("Cerrar panel").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        waitText("VisionRUT")
        compose.onNodeWithTag("bank-visionrut").assertExists()
    }

    @Test
    fun longPressSelectsAndASwipeRequiresTappingTheExposedAction() = runBlocking {
        enter()
        allExpenses()
        val dao = VisionDatabase.get(compose.activity).dao()
        val rows = expensesInMonth(dao.expenses().first(), DemoData.mesDeReferencia())
        val categorized = rows.first { it.categoryId == 1L }
        val pending = rows.first { it.categoryId == null }
        dao.editExpense(categorized.id, categorized.categoryId, "Nota conservada")
        expense(categorized.id).performTouchInput { longClick() }
        title("1 seleccionados")
        expense(pending.id).performClick()
        title("2 seleccionados")
        swipeLeft(categorized.id)
        assertEquals(categorized.categoryId, dao.expense(categorized.id)!!.categoryId)
        back()
        title("Resumen")
        allExpenses()
        swipeLeft(categorized.id)
        assertEquals(categorized.categoryId, dao.expense(categorized.id)!!.categoryId)
        compose
            .onNodeWithTag("reveal-uncategorize-${categorized.id}")
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(8_000) {
            runBlocking { dao.expense(categorized.id)!!.categoryId == null }
        }
        assertEquals("Nota conservada", dao.expense(categorized.id)!!.note)
        assertEquals(
            rows.sumOf { it.amount },
            expensesInMonth(dao.expenses().first(), DemoData.mesDeReferencia()).sumOf { it.amount },
        )
        compose.onNodeWithText("Deshacer").performClick()
        compose.waitUntil(8_000) {
            runBlocking { dao.expense(categorized.id)!!.categoryId == categorized.categoryId }
        }
        swipeLeft(pending.id)
        compose.onNodeWithTag("reveal-uncategorize-${pending.id}").assertDoesNotExist()
        assertNull(dao.expense(pending.id)!!.categoryId)
    }

    @Test
    fun deletingAReminderKeepsItsExpenseNoteAndCategory() = runBlocking {
        enter()
        val dao = VisionDatabase.get(compose.activity).dao()
        val reminder = dao.allReminders().first()
        val expense = dao.expense(reminder.expenseId)!!
        dao.editExpense(expense.id, expense.categoryId, "Cobro que se conserva")
        compose.onNodeWithTag("nav-2").performClick()
        compose.onAllNodesWithText("Editar").onFirst().performClick()
        compose.onNodeWithTag("delete-reminder").performScrollTo().performClick()
        compose.onNodeWithTag("confirm-delete-reminder").performClick()
        compose.waitUntil(8_000) { runBlocking { dao.reminder(reminder.id) == null } }
        assertEquals(expense.amount, dao.expense(expense.id)!!.amount)
        assertEquals(expense.categoryId, dao.expense(expense.id)!!.categoryId)
        assertEquals("Cobro que se conserva", dao.expense(expense.id)!!.note)
    }
}
