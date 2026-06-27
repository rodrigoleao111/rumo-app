package com.rodrigoleao.gramado2026.data.di

import android.content.Context
import com.rodrigoleao.gramado2026.data.db.TravelDatabase
import com.rodrigoleao.gramado2026.data.preferences.ContactCategoryRepository
import com.rodrigoleao.gramado2026.data.preferences.SettingsRepository
import com.rodrigoleao.gramado2026.data.preferences.settingsDataStore
import com.rodrigoleao.gramado2026.data.repository.TripRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTravelDatabase(@ApplicationContext ctx: Context): TravelDatabase =
        TravelDatabase.getInstance(ctx)

    @Provides
    @Singleton
    fun provideTripRepository(db: TravelDatabase): TripRepository = TripRepository(db)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context): SettingsRepository =
        SettingsRepository(ctx.settingsDataStore)

    @Provides
    @Singleton
    fun provideContactCategoryRepository(@ApplicationContext ctx: Context): ContactCategoryRepository =
        ContactCategoryRepository(ctx)
}
