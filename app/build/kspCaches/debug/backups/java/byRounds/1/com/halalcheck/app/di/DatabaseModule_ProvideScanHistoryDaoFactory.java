package com.halalcheck.app.di;

import com.halalcheck.app.data.local.HalalDatabase;
import com.halalcheck.app.data.local.dao.ScanHistoryDao;
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
public final class DatabaseModule_ProvideScanHistoryDaoFactory implements Factory<ScanHistoryDao> {
  private final Provider<HalalDatabase> databaseProvider;

  public DatabaseModule_ProvideScanHistoryDaoFactory(Provider<HalalDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ScanHistoryDao get() {
    return provideScanHistoryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideScanHistoryDaoFactory create(
      Provider<HalalDatabase> databaseProvider) {
    return new DatabaseModule_ProvideScanHistoryDaoFactory(databaseProvider);
  }

  public static ScanHistoryDao provideScanHistoryDao(HalalDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideScanHistoryDao(database));
  }
}
