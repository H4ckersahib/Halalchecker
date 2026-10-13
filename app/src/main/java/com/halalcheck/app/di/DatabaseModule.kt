package com.halalcheck.app.di

import android.content.Context
import androidx.room.Room
import com.halalcheck.app.data.local.HalalDatabase
import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.local.dao.ProductDao
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HalalDatabase {
        return Room.databaseBuilder(
            context,
            HalalDatabase::class.java,
            HalalDatabase.DATABASE_NAME
        )
            .addMigrations(HalalDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideIngredientDao(database: HalalDatabase): IngredientDao {
        return database.ingredientDao()
    }

    @Provides
    fun provideProductDao(database: HalalDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun provideScanHistoryDao(database: HalalDatabase): ScanHistoryDao {
        return database.scanHistoryDao()
    }
}
