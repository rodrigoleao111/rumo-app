package com.rodrigoleao.pipa.data.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.rodrigoleao.pipa.data.db.TravelDatabase
import com.rodrigoleao.pipa.data.preferences.ContactCategoryRepository
import com.rodrigoleao.pipa.data.preferences.SettingsRepository
import com.rodrigoleao.pipa.data.preferences.settingsDataStore
import com.rodrigoleao.pipa.data.repository.AiConversationRepository
import com.rodrigoleao.pipa.data.repository.TripRepository
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
    fun provideAiConversationRepository(db: TravelDatabase): AiConversationRepository =
        AiConversationRepository(db)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context): SettingsRepository =
        SettingsRepository(ctx.settingsDataStore)

    @Provides
    @Singleton
    fun provideContactCategoryRepository(@ApplicationContext ctx: Context): ContactCategoryRepository =
        ContactCategoryRepository(ctx)

    /** Instância única do Firebase Analytics para registrar eventos customizados. */
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext ctx: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(ctx)
}
