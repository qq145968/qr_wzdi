# ===== ML Kit Barcode Scanning =====
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**

# ===== Jetpack Compose =====
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
# Keep Compose lambdas
-keepclassmembers class * {
    @androidx.compose.runtime.* <methods>;
}

# ===== CameraX =====
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ===== Gson =====
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class com.google.gson.reflect.TypeToken
-keep class com.google.gson.JsonElement
-keep class com.google.gson.JsonObject
-keep class com.google.gson.JsonArray

# Keep data classes used with Gson
-keep class com.scanrobot.app.data.** { *; }
-keep class com.scanrobot.app.model.** { *; }
-keep class com.scanrobot.app.**$* { *; }

# ===== OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ===== Coil =====
-dontwarn coil.**

# ===== Kotlin metadata =====
-keepattributes KotlinMetadata
-keep class kotlin.Metadata { *; }

# ===== General =====
-keepattributes SourceFile,LineNumberTable
-dontwarn org.jetbrains.annotations.**
