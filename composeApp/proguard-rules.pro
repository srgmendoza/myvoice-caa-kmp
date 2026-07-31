# ---------------------------------------------------------------------------
# kotlinx-serialization
# https://github.com/Kotlin/kotlinx.serialization#android
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep `Companion` object fields of serializable classes so that the
# serializer can be looked up reflectively via Companion.serializer().
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Keep generated serializer classes.
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}

# ---------------------------------------------------------------------------
# Ktor
# ---------------------------------------------------------------------------
# Ktor uses java.lang.management / SLF4J classes that are absent on Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
-keep class io.ktor.client.engine.okhttp.OkHttpEngineContainer { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }

# OkHttp (Ktor Android engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------------------------------------------------------------------------
# SQLDelight (android-driver wraps androidx.sqlite; no reflection of app code)
# ---------------------------------------------------------------------------
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ---------------------------------------------------------------------------
# Koin (constructor DSL resolves definitions reflectively in some setups)
# ---------------------------------------------------------------------------
-keepattributes Signature
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ---------------------------------------------------------------------------
# Decompose / Essenty (state keeper uses kotlinx-serialization; keep
# StateKeeper-serialized classes via the @Serializable rules above)
# ---------------------------------------------------------------------------
-keep class com.arkivanov.decompose.** { *; }
-keep class com.arkivanov.essenty.** { *; }
-dontwarn com.arkivanov.decompose.**
-dontwarn com.arkivanov.essenty.**

# ---------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.debug.**

# Keep enum values()/valueOf() used reflectively (serialization of enums).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
