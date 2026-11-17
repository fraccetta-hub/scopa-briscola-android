package com.fraccetta.scopabriscola.di

import android.content.Context
import androidx.room.Room
import com.fraccetta.scopabriscola.bluetooth.BtManager
import com.fraccetta.scopabriscola.data.db.AppDatabase
import com.fraccetta.scopabriscola.data.db.GameResultDao
import com.fraccetta.scopabriscola.data.db.StatisticsDao
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import com.fraccetta.scopabriscola.data.repository.StatsRepository
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
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "scopa_briscola_db")
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideStatisticsDao(db: AppDatabase): StatisticsDao = db.statisticsDao()

    @Provides
    @Singleton
    fun provideGameResultDao(db: AppDatabase): GameResultDao = db.gameResultDao()

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext ctx: Context) = UserPreferences(ctx)

    @Provides
    @Singleton
    fun provideStatsRepository(dao: StatisticsDao, gameResultDao: GameResultDao) =
        StatsRepository(dao, gameResultDao)

    @Provides
    @Singleton
    fun provideBtManager(@ApplicationContext ctx: Context) = BtManager(ctx)
}
