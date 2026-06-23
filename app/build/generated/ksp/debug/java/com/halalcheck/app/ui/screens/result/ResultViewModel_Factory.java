package com.halalcheck.app.ui.screens.result;

import androidx.lifecycle.SavedStateHandle;
import com.halalcheck.app.data.repository.IngredientRepository;
import com.halalcheck.app.data.repository.ProductRepository;
import com.halalcheck.app.domain.engine.HalalEngine;
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
public final class ResultViewModel_Factory implements Factory<ResultViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ProductRepository> productRepositoryProvider;

  private final Provider<IngredientRepository> ingredientRepositoryProvider;

  private final Provider<HalalEngine> halalEngineProvider;

  public ResultViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ProductRepository> productRepositoryProvider,
      Provider<IngredientRepository> ingredientRepositoryProvider,
      Provider<HalalEngine> halalEngineProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.productRepositoryProvider = productRepositoryProvider;
    this.ingredientRepositoryProvider = ingredientRepositoryProvider;
    this.halalEngineProvider = halalEngineProvider;
  }

  @Override
  public ResultViewModel get() {
    return newInstance(savedStateHandleProvider.get(), productRepositoryProvider.get(), ingredientRepositoryProvider.get(), halalEngineProvider.get());
  }

  public static ResultViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ProductRepository> productRepositoryProvider,
      Provider<IngredientRepository> ingredientRepositoryProvider,
      Provider<HalalEngine> halalEngineProvider) {
    return new ResultViewModel_Factory(savedStateHandleProvider, productRepositoryProvider, ingredientRepositoryProvider, halalEngineProvider);
  }

  public static ResultViewModel newInstance(SavedStateHandle savedStateHandle,
      ProductRepository productRepository, IngredientRepository ingredientRepository,
      HalalEngine halalEngine) {
    return new ResultViewModel(savedStateHandle, productRepository, ingredientRepository, halalEngine);
  }
}
