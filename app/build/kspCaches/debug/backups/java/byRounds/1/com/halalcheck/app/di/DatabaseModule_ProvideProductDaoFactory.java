package com.halalcheck.app.di;

import com.halalcheck.app.data.local.HalalDatabase;
import com.halalcheck.app.data.local.dao.ProductDao;
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
public final class DatabaseModule_ProvideProductDaoFactory implements Factory<ProductDao> {
  private final Provider<HalalDatabase> databaseProvider;

  public DatabaseModule_ProvideProductDaoFactory(Provider<HalalDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ProductDao get() {
    return provideProductDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideProductDaoFactory create(
      Provider<HalalDatabase> databaseProvider) {
    return new DatabaseModule_ProvideProductDaoFactory(databaseProvider);
  }

  public static ProductDao provideProductDao(HalalDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideProductDao(database));
  }
}
