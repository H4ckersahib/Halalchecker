package com.halalcheck.app.data.repository;

import com.halalcheck.app.data.local.dao.ProductDao;
import com.halalcheck.app.data.local.dao.ScanHistoryDao;
import com.halalcheck.app.data.remote.OpenFoodFactsApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ProductRepository_Factory implements Factory<ProductRepository> {
  private final Provider<ProductDao> productDaoProvider;

  private final Provider<ScanHistoryDao> scanHistoryDaoProvider;

  private final Provider<OpenFoodFactsApi> openFoodFactsApiProvider;

  public ProductRepository_Factory(Provider<ProductDao> productDaoProvider,
      Provider<ScanHistoryDao> scanHistoryDaoProvider,
      Provider<OpenFoodFactsApi> openFoodFactsApiProvider) {
    this.productDaoProvider = productDaoProvider;
    this.scanHistoryDaoProvider = scanHistoryDaoProvider;
    this.openFoodFactsApiProvider = openFoodFactsApiProvider;
  }

  @Override
  public ProductRepository get() {
    return newInstance(productDaoProvider.get(), scanHistoryDaoProvider.get(), openFoodFactsApiProvider.get());
  }

  public static ProductRepository_Factory create(Provider<ProductDao> productDaoProvider,
      Provider<ScanHistoryDao> scanHistoryDaoProvider,
      Provider<OpenFoodFactsApi> openFoodFactsApiProvider) {
    return new ProductRepository_Factory(productDaoProvider, scanHistoryDaoProvider, openFoodFactsApiProvider);
  }

  public static ProductRepository newInstance(ProductDao productDao, ScanHistoryDao scanHistoryDao,
      OpenFoodFactsApi openFoodFactsApi) {
    return new ProductRepository(productDao, scanHistoryDao, openFoodFactsApi);
  }
}
