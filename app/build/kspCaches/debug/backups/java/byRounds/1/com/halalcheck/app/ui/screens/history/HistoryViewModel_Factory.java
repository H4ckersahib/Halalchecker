package com.halalcheck.app.ui.screens.history;

import com.halalcheck.app.data.repository.ProductRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<ProductRepository> productRepositoryProvider;

  public HistoryViewModel_Factory(Provider<ProductRepository> productRepositoryProvider) {
    this.productRepositoryProvider = productRepositoryProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(productRepositoryProvider.get());
  }

  public static HistoryViewModel_Factory create(
      Provider<ProductRepository> productRepositoryProvider) {
    return new HistoryViewModel_Factory(productRepositoryProvider);
  }

  public static HistoryViewModel newInstance(ProductRepository productRepository) {
    return new HistoryViewModel(productRepository);
  }
}
