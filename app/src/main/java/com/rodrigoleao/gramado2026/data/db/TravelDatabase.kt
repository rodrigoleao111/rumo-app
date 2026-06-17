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
        BoardingPassEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun dayDao(): TravelDayDao
    abstract fun activityDao(): TravelActivityDao
    abstract fun contactDao(): ContactDao
    abstract fun voucherDao(): VoucherDao
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

        fun getInstance(context: Context): TravelDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigrationFrom(1, 2) // versões iniciais sem dados reais
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
