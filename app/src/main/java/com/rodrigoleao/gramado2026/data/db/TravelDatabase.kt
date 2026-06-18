package com.rodrigoleao.gramado2026.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rodrigoleao.gramado2026.data.db.dao.*
import com.rodrigoleao.gramado2026.data.db.entity.*

@Database(
    entities = [
        TripEntity::class,
        TravelDayEntity::class,
        TravelActivityEntity::class,
        ActivityBadgeEntity::class,
        WalkStopEntity::class,
        ContactEntity::class,
        VoucherEntity::class,
        VoucherGroupEntity::class,
        BoardingPassEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun dayDao(): TravelDayDao
    abstract fun activityDao(): TravelActivityDao
    abstract fun contactDao(): ContactDao
    abstract fun voucherDao(): VoucherDao
    abstract fun voucherGroupDao(): VoucherGroupDao
    abstract fun boardingPassDao(): BoardingPassDao

    companion object {
        @Volatile private var INSTANCE: TravelDatabase? = null

        // v3 → v4: hotelPhone adicionado em trips
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN hotelPhone TEXT NOT NULL DEFAULT ''")
            }
        }

        // v4 → v5: link do dia adicionado em travel_days
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE travel_days ADD COLUMN dayLinkUrl TEXT")
                db.execSQL("ALTER TABLE travel_days ADD COLUMN dayLinkLabel TEXT NOT NULL DEFAULT ''")
            }
        }

        // v5 → v6: documento do dia adicionado em travel_days
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE travel_days ADD COLUMN dayDocumentPath TEXT")
                db.execSQL("ALTER TABLE travel_days ADD COLUMN dayDocumentName TEXT NOT NULL DEFAULT ''")
            }
        }

        // v6 → v7: cor personalizada adicionada em activity_badges
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activity_badges ADD COLUMN color TEXT")
            }
        }

        // v7 → v8: título do documento adicionado em travel_days
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE travel_days ADD COLUMN dayDocumentTitle TEXT NOT NULL DEFAULT ''")
            }
        }

        // v11 → v12: flag de voucher usado/não usado
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vouchers ADD COLUMN is_used INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v12 → v13: tipo de transporte e documento em boarding_passes
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE boarding_passes ADD COLUMN transportType TEXT NOT NULL DEFAULT 'FLIGHT'")
                db.execSQL("ALTER TABLE boarding_passes ADD COLUMN documentPath TEXT")
                db.execSQL("ALTER TABLE boarding_passes ADD COLUMN documentName TEXT NOT NULL DEFAULT ''")
            }
        }

        // v13 → v14: observações em boarding_passes
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE boarding_passes ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        // v14 → v15: sortOrder e isFavorite em contacts
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contacts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v10 → v11: preferência de agrupamento de vouchers por viagem
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN voucherSortMode TEXT NOT NULL DEFAULT 'BY_CATEGORY'")
            }
        }

        // v9 → v10: coluna sort_order em vouchers para reordenação
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vouchers ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v8 → v9: tabela de categorias de vouchers
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS voucher_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        FOREIGN KEY (tripId) REFERENCES trips(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_voucher_groups_tripId ON voucher_groups(tripId)")
            }
        }

        fun getInstance(context: Context): TravelDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .fallbackToDestructiveMigrationFrom(1, 2) // versões iniciais sem dados reais
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
