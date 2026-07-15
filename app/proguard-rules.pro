# R8 keep rules for Link Clear release builds.

# --- kotlinx.serialization ---
# The ruleset models in :core are @Serializable; R8 must keep their generated
# serializers and the companion .serializer() accessors, or RuleLoader.parse
# fails at runtime with a MissingFieldException / no-serializer error.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the generated serializer for every @Serializable type.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# The serialization runtime references these by name.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- App serializable model package (belt-and-braces) ---
-keep,includedescriptorclasses class app.linkclear.core.**$$serializer { *; }
-keepclassmembers class app.linkclear.core.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
