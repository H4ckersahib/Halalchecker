package com.halalcheck.app.di;

import com.halalcheck.app.data.remote.OpenFoodFactsApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideOpenFoodFactsApiFactory implements Factory<OpenFoodFactsApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideOpenFoodFactsApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public OpenFoodFactsApi get() {
    return provideOpenFoodFactsApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideOpenFoodFactsApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideOpenFoodFactsApiFactory(retrofitProvider);
  }

  public static OpenFoodFactsApi provideOpenFoodFactsApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOpenFoodFactsApi(retrofit));
  }
}
