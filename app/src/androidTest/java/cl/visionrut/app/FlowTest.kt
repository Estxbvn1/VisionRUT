package cl.visionrut.app

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import cl.visionrut.app.data.DemoData
import cl.visionrut.app.data.VisionDatabase
import cl.visionrut.app.domain.CalculadorResumen.expensesInMonth
import cl.visionrut.app.notifications.ReminderScheduler
import cl.visionrut.app.util.money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowTest {
    @get:Rule(order = 0) val database = DemoDatabaseRule()
    @get:Rule(order = 1)
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitSaved(text: String) {
        waitText(text)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun clickText(text: String) {
        compose.onAllNodesWithText(text).onLast().performClick()
    }

    private fun closeSheet() {
        compose.onNodeWithContentDescription("Cerrar panel").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Cerrar panel").fetchSemanticsNodes().isEmpty()
        }
        android.os.SystemClock.sleep(300)
    }

    private fun expense(id: Long): SemanticsNodeInteraction {
        compose.onNodeWithTag("expense-list").performScrollToKey(id)
        return compose.onNodeWithTag("expense-$id")
    }

    private fun summaryAction(tag: String) {
        compose.onNodeWithTag("summary-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
        compose.waitUntil(5_000) {
            compose
                .onAllNodesWithContentDescription("Cerrar panel")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun monthlyExpensesCanBeCategorizedAndReminderCreated() = runBlocking {
        waitText("VisionRUT")
        val db = VisionDatabase.get(compose.activity)
        val dao = db.dao()
        val original = dao.expenses().first()
        val current = expensesInMonth(original, DemoData.mesDeReferencia())
        val pending = current.filter { it.categoryId == null }
        val originalReminderIds = dao.allReminders().map { it.id }.toSet()
        try {

            clickText("VisionRUT")
            waitText("Por categorizar")
            compose.onNodeWithText(money(current.sumOf { it.amount })).assertIsDisplayed()

            val chart = compose.onNodeWithTag("summary-chart")
            compose.onNodeWithTag("summary-list").performScrollToNode(hasTestTag("summary-chart"))
            chart.performScrollTo().assertTextContains("Mayor gasto")
            chart.performTouchInput {
                click(androidx.compose.ui.geometry.Offset(width * .46f, height * .07f))
            }
            chart.assertTextContains("Categoría")
            compose.onNodeWithTag("summary-list").performScrollToIndex(0)

            compose.onNodeWithTag("nav-1").performClick()

            compose.onNodeWithTag("nav-2").performClick()

            compose.onNodeWithTag("nav-0").performClick()
            compose.onNodeWithContentDescription("Mes anterior").performClick()
            compose.onNodeWithText(money(expensesInMonth(original, DemoData.mesDeReferencia().minusMonths(1)).sumOf { it.amount })).assertIsDisplayed()

            compose.onNodeWithContentDescription("Mes siguiente").performClick()
            summaryAction("action-Gestionar categorías")

            clickText("Crear")
            compose.onNodeWithTag("category-name").performTextInput("Salud")
            compose.onNodeWithTag("category-name").performImeAction()
            android.os.SystemClock.sleep(350)

            clickText("Crear categoría")
            waitSaved("Categoría guardada")
            summaryAction("action-Proyección de egresos")

            closeSheet()
            compose.onNodeWithTag("nav-1").performClick()
            expense(pending.first().id).performClick()
            compose.onNodeWithTag("expense-note").performTextReplacement("Comida gato")
            compose.onNodeWithTag("expense-note").performImeAction()
            android.os.SystemClock.sleep(350)
            compose.onNodeWithTag("expense-note").assertTextContains("Comida gato")
            compose.onNodeWithTag("expense-category").performClick()
            compose.waitUntil(5_000) {
                compose
                    .onAllNodesWithTag("expense-category-choice-7")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNodeWithTag("expense-category-choice-7").performScrollTo().performClick()
            compose.onNodeWithTag("expense-note").assertTextContains("Comida gato")

            clickText("Guardar cambios")
            waitSaved("Egreso actualizado")
            assertEquals("Comida gato", dao.expense(pending.first().id)!!.note)
            assertEquals(7L, dao.expense(pending.first().id)!!.categoryId)

            dao.editExpense(pending.first().id, null, "Comida gato")
            compose.onNodeWithTag("nav-0").performClick()
            compose.onNodeWithTag("summary-list").performScrollToIndex(1)
            compose.onNodeWithTag("pending").performClick()
            clickText("Seleccionar")
            pending.forEach { expense(it.id).performClick() }

            clickText("Categorizar")
            compose.onNodeWithTag("bulk-7").performScrollTo().performClick()

            clickText("Aplicar")
            waitText("No tienes egresos por categorizar\neste mes.")
            waitSaved("Categorías actualizadas")

            assertEquals(
                current.sumOf { it.amount },
                expensesInMonth(dao.expenses().first(), DemoData.mesDeReferencia()).sumOf { it.amount },
            )
            assertEquals(
                current.filter { it.categoryId == null || it.categoryId == 7L }.sumOf { it.amount },
                expensesInMonth(dao.expenses().first(), DemoData.mesDeReferencia())
                    .filter { it.categoryId == 7L }
                    .sumOf { it.amount },
            )
            compose.onNodeWithTag("nav-2").performClick()
            clickText("Crear recordatorio")

            val subscription = current.first { it.description == DemoData.SPOTIFY }
            compose.onNodeWithTag("charge-${subscription.id}").performScrollTo().performClick()
            compose.onNodeWithTag("reminder-name").performTextInput("Servicio de prueba")
            compose.onNodeWithTag("reminder-name").performImeAction()
            android.os.SystemClock.sleep(350)

            clickText("Guardar recordatorio")
            waitSaved("Recordatorio guardado")
            assertTrue(dao.allReminders().any { it.service == "Servicio de prueba" })

            dao.categorize(current.map { it.id }, null)
            compose.onNodeWithTag("nav-0").performClick()
            waitText("Categoriza tus egresos\npara ver su distribución.")
        } finally {
            original.forEach { dao.editExpense(it.id, it.categoryId, it.note) }
            dao.categories()
                .first()
                .filter { !it.included && it.name == "Salud" }
                .forEach { dao.deleteCustomCategory(it.id) }
            dao.allReminders()
                .filter { it.id !in originalReminderIds }
                .forEach {
                    ReminderScheduler.schedule(compose.activity, it.copy(enabled = false))
                    db.openHelper.writableDatabase.execSQL(
                        "DELETE FROM recordatorios WHERE id = ?",
                        arrayOf(it.id),
                    )
                }
        }
    }
}
