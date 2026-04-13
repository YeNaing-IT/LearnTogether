# Keep Room entities
-keep class com.learntogether.data.local.entity.** { *; }

# Keep Retrofit models
-keep class com.learntogether.data.remote.** { *; }

# Keep domain models
-keep class com.learntogether.domain.model.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Exceptions
