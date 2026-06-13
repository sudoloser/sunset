# Add project specific ProGuard rules here.

# Keep data models for serialization
-keep class dev.sudoloser.sunset.data.models.** { *; }
-keep class dev.sudoloser.sunset.api.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.sudoloser.sunset.**$$serializer { *; }
-keepclassmembers class dev.sudoloser.sunset.** { *** Companion; }
-keepclasseswithmembers class dev.sudoloser.sunset.** { kotlinx.serialization.KSerializer serializer(...); }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coil
-dontwarn coil.**
