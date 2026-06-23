package com.halalcheck.app.di;

import com.halalcheck.app.data.local.dao.ProductDao;
import com.halalcheck.app.data.local.dao.ScanHistoryDao;
import com.halalcheck.app.data.remote.OpenFoodFactsApi;
import com.halalcheck.app.data.repository.ProductRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class RepositoryModule_ProvideProductRepositoryFactory implements Factory<ProductRepository> {
  private final Provider<ProductDao> productDaoProvider;

  private final Provider<ScanHistoryDao> scanHistoryDaoProvider;

  private final Provider<OpenFoodFactsApi> openFoodFactsApiProvider;

  public RepositoryModule_ProvideProductRepositoryFactory(Provider<ProductDao> productDaoProvider,
      Provider<ScanHistoryDao> scanHistoryDaoProvider,
      Provider<OpenFoodFactsApi> openFoodFactsApiProvider) {
    this.productDaoProvider = productDaoProvider;
    this.scanHistoryDaoProvider = scanHistoryDaoProvider;
    this.openFoodFactsApiProvider = openFoodFactsApiProvider;
  }

  @Override
  public ProductRepository get() {
    return provideProductRepository(productDaoProvider.get(), scanHistoryDaoProvider.get(), openFoodFactsApiProvider.get());
  }

  public static RepositoryModule_ProvideProductRepositoryFactory create(
      Provider<ProductDao> productDaoProvider, Provider<ScanHistoryDao> scanHistoryDaoProvider,
      Provider<OpenFoodFactsApi> openFoodFactsApiProvider) {
    return new RepositoryModule_ProvideProductRepositoryFactory(productDaoProvider, scanHistoryDaoProvider, openFoodFactsApiProvider);
  }

  public static ProductRepository provideProductRepository(ProductDao productDao,
      ScanHistoryDao scanHistoryDao, OpenFoodFactsApi openFoodFactsApi) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideProductRepository(productDao, scanHistoryDao, openFoodFactsApi));
  }
}
