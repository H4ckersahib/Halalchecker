# Add project specific ProGuard rules here.

# Keep Moshi adapters
-keep class com.halalcheck.app.data.remote.** { *; }
-keepclassmembers class com.halalcheck.app.data.remote.** { *; }

# Keep Room entities
-keep class com.halalcheck.app.data.local.entity.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
