# VisionRUT

Prototipo académico Android para conocer y organizar los egresos de CuentaRUT. BancoEstado aparece únicamente como referencia visual del lugar donde se propone integrar el módulo. La app se llama VisionRUT, tiene identidad propia y trabaja con datos simulados almacenados en el teléfono.

## Abrir y compilar

Abrir la carpeta `VisionRUT` en Android Studio. Utilizar el JDK 21 integrado, instalar el SDK 36 y sincronizar Gradle. El módulo ejecutable es `app`.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

## Estructura

Dentro de `app/src/main/java/cl/visionrut/app/`:

```text
MainActivity.kt                 Entrada de Android y accesos desde avisos
ReminderReceivers.kt             Recepción de alarmas y restauración de avisos
data/
  Models.kt                     Categorías, egresos, recordatorios y datos persistidos
  DemoData.kt                   Categorías iniciales y movimientos simulados
  VisionDatabase.kt             Configuración e instancia compartida de Room
  VisionDao.kt                  Consultas y escrituras de la base de datos
  Migracion1a2.kt               Conversión del esquema anterior y sus relaciones
  Migracion2a3.kt               Actualización de los movimientos de demostración reconocidos
  Migracion3a4.kt               Ajuste de montos y compras cotidianas en la cartola
  Migracion4a5.kt               Actualización de notas de demostración que conservan su texto original
  VisionRUTRepository.kt        Contrato del repositorio e implementación local
domain/
  ExpenseRules.kt               Totales, distribución, proyección y límite de notas
  ReminderCalendar.kt           Recurrencias mensuales y anuales
viewmodel/
  VisionRUTViewModel.kt          Estado observable, acciones y fábrica del ViewModel
notifications/
  ReminderNotifications.kt      Contrato de avisos, adaptador y AlarmManager
ui/
  VisionRUTApp.kt               Coordinación de pantallas, permisos y acciones
  Navigation.kt                 Reglas de Atrás, encabezado, barra inferior y paneles
  screens/
    InicioScreen.kt             Escenario bancario de referencia
    ResumenScreen.kt            Totales, gráfico y panel de proyección
    EgresosScreen.kt            Listado, filtros, gestos, edición y categorización
    CategoriasScreen.kt         Gestión de categorías, iconos y colores disponibles
    RecordatoriosScreen.kt     Listado, edición y eliminación de recordatorios
  components/
    Components.kt              Elementos visuales compartidos entre pantallas
    Icons.kt                   Iconos de navegación y categorías
  theme/
    Theme.kt                   Paleta, tipografía y tema de Material Design
util/
  Formatters.kt                Formatos de moneda y fecha
```

Cada pantalla reúne sus componentes y formularios relacionados. Los componentes exclusivos se mantienen privados cuando no necesitan utilizarse desde otro archivo; los compartidos están en `components`. Un archivo puede contener varias funciones o clases relacionadas, sin mezclar interfaz, persistencia y programación de alarmas.

Los receptores permanecen en el paquete raíz porque Android los identifica por su nombre completo. `ReminderReceivers.kt` contiene ambos receptores, conservando esa identidad para las alarmas existentes.

## Arquitectura: MVVM + Repository

```text
Usuario → Pantallas Compose → VisionRUTViewModel → VisionRUTRepository → Room / DAO
              ↑                      │
              └── VisionRUTUiState ───┘
                     StateFlow
```

- **View:** muestra el estado y envía acciones. Las pantallas no realizan consultas a Room.
- **ViewModel:** gestiona mes, filtro, selección múltiple, carga, errores y operaciones del módulo. Prepara los egresos filtrados, la distribución y la proyección. Expone `StateFlow<VisionRUTUiState>`; su estado mutable es privado.
- **Repository:** concentra las lecturas y escrituras y valida las operaciones persistidas. Su implementación utiliza Room y transacciones locales.
- **Model / Data:** representa los datos y el almacenamiento. Room exporta los esquemas a `app/schemas/` al compilar.
- **Notificaciones:** un adaptador independiente conecta las solicitudes de avisos con `AlarmManager`. El ViewModel no conserva referencias a una Activity.

`VisionRUTViewModel.kt` reúne tres declaraciones relacionadas: `VisionRUTUiState`, `VisionRUTViewModel` y `VisionRUTViewModelFactory`. La fábrica proporciona el repositorio, el adaptador de avisos y `SavedStateHandle` al ViewModel. Este último conserva filtros y selección cuando Android recrea la pantalla. Navegación, paneles abiertos y desplazamiento permanecen en la interfaz, porque son estado visual.

## Tecnologías y almacenamiento

| Elemento | Configuración y función |
|---|---|
| Android mínimo | Android 8.0 / API 26 |
| Target y Compile SDK | API 36 |
| JDK de desarrollo | JDK 21; comprobado con 21.0.10 |
| Compatibilidad Java | Código fuente y bytecode Java 17 |
| Android Studio | Panda 4, 2025.3.4 Patch 1 |
| Kotlin | 2.2.10, lenguaje principal |
| Jetpack Compose | UI declarativa; BOM 2026.02.01 |
| Lifecycle / ViewModel | 2.10.0, estado y ciclo de vida |
| Corrutinas y Flow | 1.9.0, operaciones asíncronas y estado observable |
| Room | 2.8.4, persistencia SQLite local |
| KSP | 2.3.6, procesamiento de las anotaciones Room |
| AlarmManager | Servicio Android para avisos locales |
| AGP / Gradle | 9.2.1 / 9.4.1, herramientas de compilación |

El origen de los datos es local: movimientos simulados y persistencia con Room. No hay conexión a un servicio web ni integración con cuentas bancarias reales.

## Pruebas y recursos

`app/src/test/` contiene pruebas de cálculos, navegación y ViewModel con repositorio y avisos de prueba. `app/src/androidTest/` contiene las pruebas que requieren un teléfono: interfaz, Room y notificaciones.

Los recursos necesarios están en `app/src/main/res/`. La licencia de Roboto está en `licenses/Roboto.txt`. La base local conserva notas, categorías y recordatorios en `visionrut.db`.

## Modelo de datos de la etapa 2

Room utiliza tres tablas de la aplicación en `visionrut.db` (versión 5). En Database Inspector se ven también las tablas internas de Android, SQLite y Room.

| Tabla | Campos |
| --- | --- |
| `categorias` | `id`, `nombre`, `icono`, `color`, `es_predefinida` |
| `egresos` | `id`, `fecha`, `descripcion`, `tipo_operacion`, `monto`, `categoria_id`, `nota` |
| `recordatorios` | `id`, `egreso_id`, `servicio`, `fecha_referencia`, `frecuencia`, `habilitado`, `ultimo_cobro_notificado` |

Cada tabla tiene una clave primaria entera autoincremental. Los montos se guardan en pesos chilenos y las fechas en días desde el 1 de enero de 1970. `egresos.categoria_id` admite NULL. Al eliminar una categoría propia, SQLite deja sus egresos sin categoría. `recordatorios.egreso_id` es obligatorio, único y referencia al egreso. El campo técnico `ultimo_cobro_notificado` evita repetir un aviso ya entregado.

La cartola de demostración abarca agosto, septiembre, octubre y noviembre de 2026. La persona usa CuentaRUT para sus compras diarias, salidas y servicios. Paga el arriendo y los gastos comunes desde otra cuenta. Los egresos mensuales de CuentaRUT suman entre $360.000 y $420.000. La app muestra los movimientos hasta la fecha del dispositivo y al abrirse después de noviembre presenta el último mes de la muestra. Las glosas reproducen formatos de cartola y los importes representan compras simuladas en Santiago. Cada pedido de Uber Eats o Rappi tiene una nota con el local y queda disponible para que el usuario cree su categoría de delivery. En noviembre hay además tres pagos pendientes de identificar.

La migración de datos reconoce movimientos de las muestras anteriores. Conserva filas con ediciones personales y respeta los vínculos de los recordatorios. En las aperturas siguientes, la carga por identificador conserva las notas y las categorías que el usuario haya cambiado.

La proyección de diciembre promedia los egresos de septiembre, octubre y noviembre. Si noviembre está en curso, estima su cierre multiplicando el gasto acumulado por los días del mes y dividiendo por los días transcurridos. Al seleccionar un mes anterior, la ventana se desplaza y se usan los meses disponibles. Los egresos pendientes se incluyen en «Sin categoría».

La campana «Enviar ahora» de un recordatorio activo publica el aviso de ese servicio cuando Android permite las notificaciones. El envío manual es independiente del aviso programado.
