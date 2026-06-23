package com.halalcheck.app.domain.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class HalalEngine_Factory implements Factory<HalalEngine> {
  @Override
  public HalalEngine get() {
    return newInstance();
  }

  public static HalalEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HalalEngine newInstance() {
    return new HalalEngine();
  }

  private static final class InstanceHolder {
    private static final HalalEngine_Factory INSTANCE = new HalalEngine_Factory();
  }
}
