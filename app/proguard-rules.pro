# ============================
# kotlinx.serialization rules
# ============================
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Mantém classes anotadas com @Serializable e todos os seus membros
-keep,includedescriptorclasses class com.daime.grow.data.remote.model.**$$serializer { *; }
-keepclassmembers class com.daime.grow.data.remote.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.daime.grow.data.remote.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Mantém as classes DTO completas (não apenas nomes) para deserialização funcionar
-keep class com.daime.grow.data.remote.model.** { *; }

# Regras gerais do kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# ============================
# Supabase
# ============================
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ============================
# Ktor
# ============================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.ktor.util.debug.IntellijIdeaDebugDetector
-dontwarn java.lang.management.**

# ============================
# Kotlin
# ============================
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
