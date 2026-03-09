# Supabase & Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.json.** { *; }
-keep class io.github.jan_tennert.supabase.** { *; }
-keep class io.ktor.** { *; }

# Mantém os nomes dos campos dos DTOs para o Supabase não se perder
-keepclassmembernames class com.daime.grow.data.remote.model.** { *; }
