package cl.visionrut.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Renueva los apuntes de la muestra solo si la glosa y la nota todavía coinciden con v4. */
internal object Migracion4a5 : Migration(4, 5) {
    private data class Cambio(
        val id: Long, val glosa: String, val anterior: String?, val nota: String,
        val nuevaGlosa: String? = null, val categoriaOriginal: Long? = null,
    )

    private val cambios = listOf(
        Cambio(202_608_001, "MERPAGO*RAMONDIAZ", "Papas y zanahorias de la feria", "papas y zanahorias"),
        Cambio(202_608_002, "MERPAGO*ISAMORA", "Dos libros usados en la feria", "2 libros de la feria"),
        Cambio(202_608_003, "LIDER EXPRESS STA ISABEL", "Despensa y pollo para congelar", "pollo pa congelar y mercadería"),
        Cambio(202_608_005, "Servipag (www.servipag.com)", "Luz de julio", "Enel julio"),
        Cambio(202_608_007, "ALMACEN JUANITA", "Huevos, queso y pan amasado", "huevos, queso y pan amasado"),
        Cambio(202_608_008, "PAYU *UBER TRIP SANTIAGO CHL", "Viaje a casa desde donde la Dani", "uber de vuelta"),
        Cambio(202_608_009, "DL RAPPI CHILE RAPPI", "Pad thai de Thai Express", "Thai Express, pad thai"),
        Cambio(202_608_011, "TBK COMERCIAL VARGAS", "Tendedero plegable para el baño", "tendedero chico"),
        Cambio(202_608_013, "MERPAGO*MERCADOLIBRE", "Zapatillas para salir a correr", "zapatillas pa trotar"),
        Cambio(202_608_014, "TBK CINEPLANET", "Dos entradas, salimos con la Dani", "2 entradas al cine"),
        Cambio(202_608_016, "WOM PAGO EN LINEA", "Fibra del depto.", "Mundo Pacífico", "SERVIPAG.COM", 3),
        Cambio(202_608_017, "PASAJE QR RED", "Metro en horario valle", "metro con QR"),
        Cambio(202_608_019, "TEF A NICO R", "Mi parte del pool y las bebidas", "mi parte del pool"),
        Cambio(202_608_020, "DL*UBEREATS SANTIAGO CHL", "Pizza de Los Insaciables", "Los Insaciables, una familiar"),
        Cambio(202_608_021, "Spotify P48B7B82B3", "Premium individual", "premium del mes"),
        Cambio(202_608_023, "PET HAPPY", "Saco chico de comida del gato", "2 kg de pellet"),
        Cambio(202_609_001, "SANTA ISABEL ÑUÑOA", "Compra grande del mes", "súper del mes"),
        Cambio(202_609_002, "TEF A DANIELA M", "Mi parte del regalo para Nico", "cuota regalo cumple"),
        Cambio(202_609_003, "MERPAGO*JUANITASPA", "Pan, yogur y dos paltas", "pan, yogur y 2 paltas"),
        Cambio(202_609_004, "BIP! CARGA WEB", "Saldo para ir al trabajo", "carga bip pa la pega"),
        Cambio(202_609_005, "Servipag (www.servipag.com)", "Boleta de luz", "Enel agosto"),
        Cambio(202_609_007, "MERPAGO*ALIPAYSINGAPOREC", "Soporte de manubrio para el celu", "soporte del celu pa la bici"),
        Cambio(202_609_009, "MERPAGO*SOFIAR", "Guirnalda y vasos para el asado", "vasos y banderitas pal 18"),
        Cambio(202_609_010, "UBER RIDES", "A la casa del Nico", "ida a la junta"),
        Cambio(202_609_011, "DL*UBEREATS SANTIAGO CHL", "Sushi del Niu para dos", "Niu Sushi, promo de rolls"),
        Cambio(202_609_012, "WOM PAGO EN LINEA", null, "Mundo Pacífico, fibra", "SERVIPAG.COM", 3),
        Cambio(202_609_013, "UNIMARC IRARRAZAVAL", "Carne y ensaladas para el asado", "carne y pebre pal 18"),
        Cambio(202_609_014, "COPEC SAN BERNARDO", "Bencina 93, viaje al sur", "93 pa la ida al sur"),
        Cambio(202_609_015, "MERPAGO*MARISOLP", "Empanadas para el Dieciocho", "empanadas de pino"),
        Cambio(202_609_016, "TBK SOC RECREATIVA", "Entrada a la fonda", "entrada fonda"),
        Cambio(202_609_017, "RUTA 5 SUR TALCA-CHILLAN SURVIAS", "Peajes del viaje con la Dani", "TAG Survías, Talca-Chillán", "PAGO EN LINEA AUTOPISTAS", 2),
        Cambio(202_609_020, "MERPAGO*PATRICIOS", "Arena para el gato", "arena gato 5 kg"),
        Cambio(202_609_022, "PASAJE QR RED", "Metro del domingo, pagué con QR", "metro domingo, QR"),
        Cambio(202_610_001, "MERPAGO*RAMONDIAZ", "Frutillas y plátanos de la feria", "frutillas y plátanos"),
        Cambio(202_610_002, "MERPAGO*LUCASG", "Marraquetas y queso de la panadería", "marraquetas y queso"),
        Cambio(202_610_003, "LIDER.CL", "Despensa con retiro en tienda", "pedido del Líder, retiro"),
        Cambio(202_610_004, "PASAJE QR RED", "Vuelta en Metro del domingo", "metro de vuelta"),
        Cambio(202_610_005, "SERVIPAG.COM", "Luz del depto.", "luz septiembre"),
        Cambio(202_610_007, "PAYU *UBER EATS", "Falafel de El Árabe", "El Árabe, falafel"),
        Cambio(202_610_008, "MERPAGO*MERCADOLI Las Condes CHL", "Perfume de regalo para la Dani", "perfume pa regalo"),
        Cambio(202_610_011, "BCI PAGOS *M ROJAS", "Ajuste del pantalón en la sastrería", "basta del pantalón"),
        Cambio(202_610_012, "DL RAPPI CHILE RAPPI", "Hamburguesa de La Burguesía", "La Burguesía, promo"),
        Cambio(202_610_013, "BCI PAGOS *J PARRA", "Pan, jamón y café del almacén", "pan y jamón pa la once"),
        Cambio(202_610_014, "UBER *TRIP", "Viaje a casa", "uber pa la casa"),
        Cambio(202_610_015, "Merpago*mcdonalds las condes", "Dos combos en el local", "2 combos del Mc"),
        Cambio(202_610_016, "WOM PAGO EN LINEA", null, "Mundo Pacífico", "SERVIPAG.COM", 3),
        Cambio(202_610_018, "TEF A NICO R", "Mi parte de la cancha", "cuota pichanga"),
        Cambio(202_610_020, "PAYU *UBER TRIP SANTIAGO CHL", "Volví tarde del cumpleaños", "vuelta del cumple"),
        Cambio(202_610_021, "Spotify P48B7B82B3", "Premium individual", "premium del mes"),
        Cambio(202_610_023, "DL*UBEREATS SANTIAGO CHL", "Dos ramen de Momotaro", "Momotaro, 2 ramen"),
        Cambio(202_610_024, "PET HAPPY", "Alimento y un juguete", "pellet y ratón de juguete"),
        Cambio(202_610_026, "MERPAGO*NICHOSTORE", "Polera para la oficina", "polera pa la pega"),
        Cambio(202_610_029, "RAPPI CHILE", "Arepa de Ávila", "Ávila, arepa de pollo"),
        Cambio(202_610_031, "TEF A DANIELA M", "Mitad de los tragos del viernes", "mitad de los schop"),
        Cambio(202_610_080, "GOOGLE *Google One", "Respaldo anual de fotos", "espacio pa las fotos"),
        Cambio(202_611_002, "LIDER EXPRESS STA ISABEL", "Arroz, legumbres, pollo y aseo", "arroz, porotos y detergente"),
        Cambio(202_611_003, "MERPAGO*PAULAM", "Café y tostadas después de la pega", "cortado y tostadas"),
        Cambio(202_611_005, "ALMACEN JUANITA", "Huevos, pan y queso para la once", "pan, queso y huevos"),
        Cambio(202_611_006, "Netflix.com 866-579-7172", "Plan de Netflix del depto.", "plan básico"),
        Cambio(202_611_007, "Payu *uber eats", "Burrito de Mexa con la Dani", "Mexa, burrito"),
        Cambio(202_611_008, "Servipag (www.servipag.com)", "Cuenta de luz de octubre", "Enel octubre"),
        Cambio(202_611_009, "Uber rides", "Viaje a casa", "vuelta del carrete"),
        Cambio(202_611_010, "MERPAGO*RAMONDIAZ", "Tomates y paltas de la feria", "tomate y palta"),
        Cambio(202_611_012, "Merpago*mcdonalds", "Almuerzo en el local con Nico", "almuerzo en el Mc"),
        Cambio(202_611_014, "TBK BOWLING PLAZA", "Una partida de bowling con Nico", "dos schop y papas pa picar", "TBK FUENTE DE SODA", 5),
        Cambio(202_611_015, "PASAJE QR RED", "Metro en hora punta, pagué con QR", "metro punta, QR"),
        Cambio(202_611_016, "MERCADOPAGO *MERCADOL", "Zapatillas para el trabajo", "zapatillas pa la pega"),
        Cambio(202_611_018, "DL RAPPI CHILE RAPPI", "Pizza de Da Dino", "Da Dino, pizza familiar"),
        Cambio(202_611_019, "WOM PAGO EN LINEA", "Internet de la casa", "internet Mundo Pacífico", "SERVIPAG.COM", 3),
        Cambio(202_611_022, "PAYU *UBER TRIP SANTIAGO CHL", "De la oficina al dentista", "uber al dentista"),
        Cambio(202_611_023, "COPEC IRARRAZAVAL", "12 litros de 93", "12 L de 93"),
        Cambio(202_611_024, "TEF A NICO R", "Cuota del asado con los amigos", "cuota pal asado"),
        Cambio(202_611_026, "UNIMARC IRARRAZAVAL", "Leche, fideos, atún y verduras", "leche, fideos y atún"),
        Cambio(202_611_029, "MERPAGO*ISA VERA", "Novela usada de la feria del libro", "libro usado de la feria"),
        Cambio(202_611_030, "PET HAPPY IRARRAZAVAL", "Arena para el gato", "5k arena gato"),
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        for (c in cambios) {
            db.execSQL("""UPDATE egresos SET nota=?, descripcion=COALESCE(?, descripcion)
                WHERE id=? AND descripcion=? AND nota IS ? AND (? IS NULL OR categoria_id IS ?)""",
                arrayOf(c.nota, c.nuevaGlosa, c.id, c.glosa, c.anterior, c.categoriaOriginal, c.categoriaOriginal))
        }
    }
}
