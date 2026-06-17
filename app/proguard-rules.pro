# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Behalte Debugging-Informationen bei
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Allgemeine Android-Regeln
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keep public class * extends java.lang.Exception

# Androidx und Support Bibliotheken
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep public class * extends androidx.**

# Aktivitäten, Services, etc.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Behalte View Getter/Setter
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.viewmodel.compose.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# App-spezifische ViewModels
-keep class com.lakeshorestudios.nextwave.ui.** { *; }
-keep class com.lakeshorestudios.nextwave.ui.home.HomeViewModel { *; }
-keep class com.lakeshorestudios.nextwave.ui.departures.DeparturesViewModel { *; }
-keep class com.lakeshorestudios.nextwave.ui.settings.SettingsViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Kotlin Reflection
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Koin für die Abhängigkeitsinjektion, falls verwendet
-keep class org.koin.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# GSON
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Koroutinen
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Datenmodelle der App
-keep class com.lakeshorestudios.nextwave.data.models.** { *; }
-keep class com.lakeshorestudios.nextwave.domain.model.** { *; }
-keep class com.lakeshorestudios.nextwave.data.repository.** { *; }
-keep class com.lakeshorestudios.nextwave.data.api.** { *; }

# Coil
-keep class coil.** { *; }

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / Supabase
-dontwarn io.ktor.**
-dontwarn io.github.jan.**
-keepclassmembers class io.ktor.** { *; }

# slf4j (optional logging binder pulled in transitively by Ktor/Supabase; safe to ignore)
-dontwarn org.slf4j.**