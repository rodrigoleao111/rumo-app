package com.rodrigoleao.gramado2026.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.rodrigoleao.gramado2026.data.db.entity.VoucherGroupEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Testa a cadeia completa de migrations v3 → v16 contra o schema real das entities.
 *
 * Estratégia: os schemas JSON das versões 3–15 nunca foram exportados
 * (`exportSchema` era `false` até a v16), então o `MigrationTestHelper` não consegue
 * criar um banco em versão antiga. Em vez disso:
 *
 *  1. Cria um arquivo SQLite real com o schema da v3, escrito à mão — derivado do
 *     schema atual (16.json) revertendo cada migration (todas são ADD COLUMN /
 *     CREATE TABLE aditivas);
 *  2. Semeia uma linha em cada tabela;
 *  3. Abre o banco com Room + `ALL_MIGRATIONS` — o Room executa as 13 migrations e
 *     **valida o schema resultante contra as entities anotadas**. Qualquer coluna
 *     errada/faltante lança `IllegalStateException("Migration didn't properly handle…")`,
 *     exatamente o erro que ocorreria em produção;
 *  4. Verifica que os dados semeados sobreviveram e que os DEFAULTs das migrations
 *     foram aplicados.
 *
 * > Migrations futuras (16 → N) devem usar `MigrationTestHelper` — o schema 16.json
 * > agora é exportado em `app/schemas/` e está disponível como asset do androidTest.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test.db"
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbFile: File get() = context.getDatabasePath(TEST_DB)
    private var roomDb: TravelDatabase? = null

    // A partir da v16 os schemas JSON são exportados (app/schemas/, expostos como asset
    // do androidTest), então migrations novas podem usar o MigrationTestHelper padrão.
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TravelDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun deleteExistingDb() {
        roomDb?.close()
        SQLiteDatabase.deleteDatabase(dbFile)
        dbFile.parentFile?.mkdirs()
    }

    @After
    fun tearDown() {
        roomDb?.close()
        SQLiteDatabase.deleteDatabase(dbFile)
    }

    // ── Schema v3 (derivado do 16.json revertendo as migrations 3→16) ────────────

    private fun createV3Database(seed: Boolean) {
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.use {
            // trips: v16 menos hotelPhone (3→4) e voucherSortMode (10→11)
            it.execSQL(
                """CREATE TABLE `trips` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `name` TEXT NOT NULL, `destination` TEXT NOT NULL, `coverEmoji` TEXT NOT NULL,
                   `hotelName` TEXT NOT NULL, `hotelAddress` TEXT NOT NULL,
                   `startDate` TEXT, `endDate` TEXT, `createdAt` INTEGER NOT NULL,
                   `latitude` REAL, `longitude` REAL)"""
            )
            // travel_days: v16 menos dayLinkUrl/dayLinkLabel (4→5),
            // dayDocumentPath/dayDocumentName (5→6), dayDocumentTitle (7→8)
            it.execSQL(
                """CREATE TABLE `travel_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `tripId` INTEGER NOT NULL, `dayNumber` INTEGER NOT NULL, `date` TEXT NOT NULL,
                   `dayOfWeek` TEXT NOT NULL, `title` TEXT NOT NULL, `weatherEmoji` TEXT NOT NULL,
                   `minTemp` INTEGER NOT NULL, `maxTemp` INTEGER NOT NULL,
                   `weatherCondition` TEXT NOT NULL, `dayAlert` TEXT,
                   FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            it.execSQL("CREATE INDEX `index_travel_days_tripId` ON `travel_days` (`tripId`)")
            // travel_activities: nenhuma migration alterou — igual à v16
            it.execSQL(
                """CREATE TABLE `travel_activities` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `dayId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `time` TEXT NOT NULL,
                   `emoji` TEXT NOT NULL, `name` TEXT NOT NULL, `detail` TEXT NOT NULL,
                   `mapQuery` TEXT, `uberDestination` TEXT,
                   FOREIGN KEY(`dayId`) REFERENCES `travel_days`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            it.execSQL("CREATE INDEX `index_travel_activities_dayId` ON `travel_activities` (`dayId`)")
            // activity_badges: v16 menos color (6→7)
            it.execSQL(
                """CREATE TABLE `activity_badges` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `activityId` INTEGER NOT NULL, `badgeType` TEXT NOT NULL, `label` TEXT NOT NULL,
                   FOREIGN KEY(`activityId`) REFERENCES `travel_activities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            it.execSQL("CREATE INDEX `index_activity_badges_activityId` ON `activity_badges` (`activityId`)")
            // walk_stops: nenhuma migration alterou — igual à v16
            it.execSQL(
                """CREATE TABLE `walk_stops` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `activityId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `emoji` TEXT NOT NULL,
                   `label` TEXT NOT NULL, `sublabel` TEXT, `isLast` INTEGER NOT NULL,
                   FOREIGN KEY(`activityId`) REFERENCES `travel_activities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            it.execSQL("CREATE INDEX `index_walk_stops_activityId` ON `walk_stops` (`activityId`)")
            // contacts: v16 menos sortOrder/isFavorite (14→15) e customTypeName (15→16)
            it.execSQL(
                """CREATE TABLE `contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `tripId` INTEGER NOT NULL, `name` TEXT NOT NULL, `role` TEXT NOT NULL,
                   `phone` TEXT, `contactType` TEXT NOT NULL, `hasWhatsApp` INTEGER NOT NULL,
                   `isEmergency` INTEGER NOT NULL,
                   FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            it.execSQL("CREATE INDEX `index_contacts_tripId` ON `contacts` (`tripId`)")
            // vouchers: v16 menos sort_order (9→10) e is_used (11→12)
            it.execSQL(
                """CREATE TABLE `vouchers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `tripId` INTEGER NOT NULL, `dayNumber` INTEGER, `emoji` TEXT NOT NULL,
                   `groupName` TEXT NOT NULL, `name` TEXT NOT NULL, `person` TEXT,
                   `assetPath` TEXT NOT NULL,
                   FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            it.execSQL("CREATE INDEX `index_vouchers_tripId` ON `vouchers` (`tripId`)")
            // voucher_groups: NÃO existe na v3 — criada pela migration 8→9
            // boarding_passes: v16 menos transportType/documentPath/documentName (12→13) e notes (13→14)
            it.execSQL(
                """CREATE TABLE `boarding_passes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `tripId` INTEGER NOT NULL, `origin` TEXT NOT NULL, `originCity` TEXT NOT NULL,
                   `destination` TEXT NOT NULL, `destinationCity` TEXT NOT NULL,
                   `flightNumber` TEXT NOT NULL, `date` TEXT NOT NULL, `boardingTime` TEXT NOT NULL,
                   `passenger` TEXT NOT NULL, `walletUrl` TEXT,
                   FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"""
            )
            it.execSQL("CREATE INDEX `index_boarding_passes_tripId` ON `boarding_passes` (`tripId`)")

            if (seed) {
                it.execSQL(
                    """INSERT INTO trips (id, name, destination, coverEmoji, hotelName, hotelAddress,
                       startDate, endDate, createdAt, latitude, longitude)
                       VALUES (1, 'Gramado & Canela', 'Gramado, RS', '⛰️', 'Hotel San Lucas',
                       'Rua João Carniel, 73', '2026-06-09', '2026-06-13', 1700000000000, -29.37, -50.87)"""
                )
                it.execSQL(
                    """INSERT INTO travel_days (id, tripId, dayNumber, date, dayOfWeek, title,
                       weatherEmoji, minTemp, maxTemp, weatherCondition, dayAlert)
                       VALUES (1, 1, 1, '2026-06-09', 'Terça-feira', 'Chegada', '☀️', 8, 18, 'Ensolarado', 'Levar casaco')"""
                )
                it.execSQL(
                    """INSERT INTO travel_activities (id, dayId, position, time, emoji, name, detail, mapQuery, uberDestination)
                       VALUES (1, 1, 0, '14h00', '🏨', 'Check-in', 'Hotel San Lucas', 'Hotel San Lucas Gramado', 'Hotel San Lucas Gramado')"""
                )
                it.execSQL("INSERT INTO activity_badges (id, activityId, badgeType, label) VALUES (1, 1, 'BOOKED', 'RESERVADO')")
                it.execSQL(
                    """INSERT INTO walk_stops (id, activityId, position, emoji, label, sublabel, isLast)
                       VALUES (1, 1, 0, '🚶', 'Rua Coberta', '200m', 1)"""
                )
                it.execSQL(
                    """INSERT INTO contacts (id, tripId, name, role, phone, contactType, hasWhatsApp, isEmergency)
                       VALUES (1, 1, 'Guia Ana', 'Agência Brocker', '54999990000', 'AGENCY', 1, 0)"""
                )
                it.execSQL(
                    """INSERT INTO vouchers (id, tripId, dayNumber, emoji, groupName, name, person, assetPath)
                       VALUES (1, 1, 2, '🎫', 'Passeios', 'Bondinhos Aéreos', 'Rodrigo', 'vouchers/bondinho.pdf')"""
                )
                it.execSQL(
                    """INSERT INTO boarding_passes (id, tripId, origin, originCity, destination, destinationCity,
                       flightNumber, date, boardingTime, passenger, walletUrl)
                       VALUES (1, 1, 'REC', 'Recife', 'POA', 'Porto Alegre', 'AD4104', '09/06', '05h30', 'Rodrigo', NULL)"""
                )
            }

            it.version = 3
        }
    }

    private fun openWithMigrations(): TravelDatabase =
        Room.databaseBuilder(context, TravelDatabase::class.java, TEST_DB)
            .addMigrations(*TravelDatabase.ALL_MIGRATIONS)
            .build()
            .also { roomDb = it }

    // ── Testes ────────────────────────────────────────────────────────────────

    /** Banco v3 vazio migra até a v16 e o Room valida o schema final contra as entities. */
    @Test
    fun migracaoDe3Para16_schemaValidadoPeloRoom() {
        createV3Database(seed = false)
        val db = openWithMigrations()
        // Qualquer query dispara open + migrations + validação de schema
        runBlocking { assertThat(db.tripDao().count()).isEqualTo(0) }
    }

    /** Dados existentes sobrevivem à cadeia completa e os DEFAULTs das migrations são aplicados. */
    @Test
    fun migracaoDe3Para16_preservaDadosEAplicaDefaults() {
        createV3Database(seed = true)
        val db = openWithMigrations()

        runBlocking {
            // trips — hotelPhone (3→4) e voucherSortMode (10→11)
            val trip = db.tripDao().getById(1)!!
            assertThat(trip.name).isEqualTo("Gramado & Canela")
            assertThat(trip.hotelPhone).isEmpty()
            assertThat(trip.voucherSortMode).isEqualTo("BY_CATEGORY")
            assertThat(trip.latitude).isEqualTo(-29.37)

            // travel_days — dayLink* (4→5), dayDocument* (5→6, 7→8)
            val day = db.dayDao().getById(1)!!
            assertThat(day.title).isEqualTo("Chegada")
            assertThat(day.dayAlert).isEqualTo("Levar casaco")
            assertThat(day.dayLinkUrl).isNull()
            assertThat(day.dayLinkLabel).isEmpty()
            assertThat(day.dayDocumentPath).isNull()
            assertThat(day.dayDocumentName).isEmpty()
            assertThat(day.dayDocumentTitle).isEmpty()

            // travel_activities — intocada pelas migrations
            val activity = db.activityDao().getById(1)!!
            assertThat(activity.name).isEqualTo("Check-in")
            assertThat(activity.mapQuery).isEqualTo("Hotel San Lucas Gramado")

            // activity_badges — color (6→7)
            val badges = db.activityDao().getBadgesForActivity(1)
            assertThat(badges).hasSize(1)
            assertThat(badges[0].label).isEqualTo("RESERVADO")
            assertThat(badges[0].color).isNull()

            // walk_stops — intocada
            val stops = db.activityDao().getWalkStopsForActivity(1)
            assertThat(stops).hasSize(1)
            assertThat(stops[0].label).isEqualTo("Rua Coberta")

            // contacts — sortOrder/isFavorite (14→15), customTypeName (15→16)
            val contact = db.contactDao().getById(1)!!
            assertThat(contact.name).isEqualTo("Guia Ana")
            assertThat(contact.sortOrder).isEqualTo(0)
            assertThat(contact.isFavorite).isFalse()
            assertThat(contact.customTypeName).isEmpty()

            // vouchers — sort_order (9→10), is_used (11→12)
            val voucher = db.voucherDao().getById(1)!!
            assertThat(voucher.name).isEqualTo("Bondinhos Aéreos")
            assertThat(voucher.sortOrder).isEqualTo(0)
            assertThat(voucher.isUsed).isFalse()

            // boarding_passes — transportType/document* (12→13), notes (13→14)
            val pass = db.boardingPassDao().getById(1)!!
            assertThat(pass.flightNumber).isEqualTo("AD4104")
            assertThat(pass.transportType).isEqualTo("FLIGHT")
            assertThat(pass.documentPath).isNull()
            assertThat(pass.documentName).isEmpty()
            assertThat(pass.notes).isEmpty()
        }
    }

    /** A tabela voucher_groups (criada pela migration 8→9) aceita insert com FK funcionando. */
    @Test
    fun migracaoDe3Para16_tabelaVoucherGroupsCriadaEFuncional() {
        createV3Database(seed = true)
        val db = openWithMigrations()

        runBlocking {
            val id = db.voucherGroupDao().insert(VoucherGroupEntity(tripId = 1, name = "Passeios"))
            assertThat(id).isGreaterThan(0)
            val groups = db.voucherGroupDao().getForTrip(1)
            assertThat(groups).hasSize(1)
            assertThat(groups[0].name).isEqualTo("Passeios")
        }
    }

    // ── F1: migração 16 → 17 via MigrationTestHelper (16.json já exportado) ───────

    /**
     * Cria um banco v16 com uma viagem, aplica a 16→17 e valida o schema resultante
     * contra as entities. Confirma que os DEFAULTs (tripUuid='', lastEditedAt=0) são
     * aplicados à linha existente — nenhum dado é perdido (F1, UC de migração).
     */
    @Test
    fun migracao16Para17_adicionaUuidETimestampComDefaults() {
        val migrationDb = "migration-16-17.db"
        migrationHelper.createDatabase(migrationDb, 16).apply {
            execSQL(
                """INSERT INTO trips (id, name, destination, coverEmoji, hotelName, hotelAddress,
                   hotelPhone, startDate, endDate, createdAt, latitude, longitude, voucherSortMode)
                   VALUES (1, 'Gramado & Canela', 'Gramado, RS', '⛰️', 'Hotel San Lucas',
                   'Rua João Carniel, 73', '', '2026-06-09', '2026-06-13', 1700000000000,
                   -29.37, -50.87, 'BY_CATEGORY')"""
            )
            close()
        }

        // runMigrationsAndValidate valida o schema final contra o 17.json/entities
        val db = migrationHelper.runMigrationsAndValidate(
            migrationDb, 17, true, *TravelDatabase.ALL_MIGRATIONS
        )

        db.query("SELECT name, tripUuid, lastEditedAt FROM trips WHERE id = 1").use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(c.getString(0)).isEqualTo("Gramado & Canela")  // dado preservado
            assertThat(c.getString(1)).isEqualTo("")                  // default tripUuid
            assertThat(c.getLong(2)).isEqualTo(0L)                    // default lastEditedAt
        }
    }

    /**
     * F4: a migração 17→18 cria as tabelas de notas (notes, note_blocks, checklist_items).
     * `runMigrationsAndValidate` valida o schema resultante contra o 18.json; um insert
     * confirma que a nova tabela e sua FK para trips funcionam.
     */
    @Test
    fun migracao17Para18_criaTabelasDeNotas() {
        val migrationDb = "migration-17-18.db"
        migrationHelper.createDatabase(migrationDb, 17).apply {
            execSQL(
                """INSERT INTO trips (id, name, destination, coverEmoji, hotelName, hotelAddress,
                   hotelPhone, startDate, endDate, createdAt, latitude, longitude, voucherSortMode,
                   tripUuid, lastEditedAt)
                   VALUES (1, 'Gramado', 'Gramado, RS', '⛰️', '', '', '', '2026-06-09', '2026-06-09',
                   1700000000000, NULL, NULL, 'BY_CATEGORY', 'uuid-1', 1700000000000)"""
            )
            close()
        }

        val db = migrationHelper.runMigrationsAndValidate(
            migrationDb, 18, true, *TravelDatabase.ALL_MIGRATIONS
        )

        // Insert numa das novas tabelas confirma criação + FK operacional
        db.execSQL(
            "INSERT INTO notes (tripId, dayId, title, sortOrder, createdAt, updatedAt) " +
                "VALUES (1, NULL, 'Packing list', 0, 1700000000000, 1700000000000)"
        )
        db.query("SELECT title FROM notes WHERE tripId = 1").use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(c.getString(0)).isEqualTo("Packing list")
        }
    }
}
