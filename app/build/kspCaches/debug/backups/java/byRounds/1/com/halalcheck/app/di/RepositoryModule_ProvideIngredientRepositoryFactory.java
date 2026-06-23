package com.halalcheck.app.di;

import com.halalcheck.app.data.local.dao.IngredientDao;
import com.halalcheck.app.data.repository.IngredientRepository;
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
public final class RepositoryModule_ProvideIngredientRepositoryFactory implements Factory<IngredientRepository> {
  private final Provider<IngredientDao> ingredientDaoProvider;

  public RepositoryModule_ProvideIngredientRepositoryFactory(
      Provider<IngredientDao> ingredientDaoProvider) {
    this.ingredientDaoProvider = ingredientDaoProvider;
  }

  @Override
  public IngredientRepository get() {
    return provideIngredientRepository(ingredientDaoProvider.get());
  }

  public static RepositoryModule_ProvideIngredientRepositoryFactory create(
      Provider<IngredientDao> ingredientDaoProvider) {
    return new RepositoryModule_ProvideIngredientRepositoryFactory(ingredientDaoProvider);
  }

  public static IngredientRepository provideIngredientRepository(IngredientDao ingredientDao) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideIngredientRepository(ingredientDao));
  }
}
