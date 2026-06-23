-if class com.halalcheck.app.data.remote.OpenFoodFactsResponse
-keepnames class com.halalcheck.app.data.remote.OpenFoodFactsResponse
-if class com.halalcheck.app.data.remote.OpenFoodFactsResponse
-keep class com.halalcheck.app.data.remote.OpenFoodFactsResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.halalcheck.app.data.remote.OpenFoodFactsResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.halalcheck.app.data.remote.OpenFoodFactsResponse
-keepclassmembers class com.halalcheck.app.data.remote.OpenFoodFactsResponse {
    public synthetic <init>(int,java.lang.String,com.halalcheck.app.data.remote.OpenFoodFactsProduct,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
