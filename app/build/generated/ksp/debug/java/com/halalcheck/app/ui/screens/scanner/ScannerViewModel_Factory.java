package com.halalcheck.app.ui.screens.scanner;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ScannerViewModel_Factory implements Factory<ScannerViewModel> {
  @Override
  public ScannerViewModel get() {
    return newInstance();
  }

  public static ScannerViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScannerViewModel newInstance() {
    return new ScannerViewModel();
  }

  private static final class InstanceHolder {
    private static final ScannerViewModel_Factory INSTANCE = new ScannerViewModel_Factory();
  }
}
