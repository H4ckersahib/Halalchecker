package com.halalcheck.app.di;

import com.halalcheck.app.data.local.HalalDatabase;
import com.halalcheck.app.data.local.dao.IngredientDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideIngredientDaoFactory implements Factory<IngredientDao> {
  private final Provider<HalalDatabase> databaseProvider;

  public DatabaseModule_ProvideIngredientDaoFactory(Provider<HalalDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public IngredientDao get() {
    return provideIngredientDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideIngredientDaoFactory create(
      Provider<HalalDatabase> databaseProvider) {
    return new DatabaseModule_ProvideIngredientDaoFactory(databaseProvider);
  }

  public static IngredientDao provideIngredientDao(HalalDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideIngredientDao(database));
  }
}
