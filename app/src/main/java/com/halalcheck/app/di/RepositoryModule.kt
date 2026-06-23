package com.halalcheck.app.di

import com.halalcheck.app.data.local.dao.IngredientDao
import com.halalcheck.app.data.local.dao.ProductDao
import com.halalcheck.app.data.local.dao.ScanHistoryDao
import com.halalcheck.app.data.remote.OpenFoodFactsApi
import com.halalcheck.app.data.repository.IngredientRepository
import com.halalcheck.app.data.repository.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProductRepository(
        productDao: ProductDao,
        scanHistoryDao: ScanHistoryDao,
        openFoodFactsApi: OpenFoodFactsApi
    ): ProductRepository {
        return ProductRepository(productDao, scanHistoryDao, openFoodFactsApi)
    }

    @Provides
    @Singleton
    fun provideIngredientRepository(
        ingredientDao: IngredientDao
    ): IngredientRepository {
        return IngredientRepository(ingredientDao)
    }
}
