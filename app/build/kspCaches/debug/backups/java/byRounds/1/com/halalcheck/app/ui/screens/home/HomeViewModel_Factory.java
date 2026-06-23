package com.halalcheck.app.ui.screens.home;

import com.halalcheck.app.data.repository.IngredientRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ProductRepository> productRepositoryProvider;

  private final Provider<IngredientRepository> ingredientRepositoryProvider;

  public HomeViewModel_Factory(Provider<ProductRepository> productRepositoryProvider,
      Provider<IngredientRepository> ingredientRepositoryProvider) {
    this.productRepositoryProvider = productRepositoryProvider;
    this.ingredientRepositoryProvider = ingredientRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(productRepositoryProvider.get(), ingredientRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<ProductRepository> productRepositoryProvider,
      Provider<IngredientRepository> ingredientRepositoryProvider) {
    return new HomeViewModel_Factory(productRepositoryProvider, ingredientRepositoryProvider);
  }

  public static HomeViewModel newInstance(ProductRepository productRepository,
      IngredientRepository ingredientRepository) {
    return new HomeViewModel(productRepository, ingredientRepository);
  }
}
