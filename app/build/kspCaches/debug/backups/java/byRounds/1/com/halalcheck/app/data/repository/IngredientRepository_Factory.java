package com.halalcheck.app.data.repository;

import com.halalcheck.app.data.local.dao.IngredientDao;
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
public final class IngredientRepository_Factory implements Factory<IngredientRepository> {
  private final Provider<IngredientDao> ingredientDaoProvider;

  public IngredientRepository_Factory(Provider<IngredientDao> ingredientDaoProvider) {
    this.ingredientDaoProvider = ingredientDaoProvider;
  }

  @Override
  public IngredientRepository get() {
    return newInstance(ingredientDaoProvider.get());
  }

  public static IngredientRepository_Factory create(Provider<IngredientDao> ingredientDaoProvider) {
    return new IngredientRepository_Factory(ingredientDaoProvider);
  }

  public static IngredientRepository newInstance(IngredientDao ingredientDao) {
    return new IngredientRepository(ingredientDao);
  }
}
