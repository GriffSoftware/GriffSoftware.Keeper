# Room and Hilt generate code that is referenced reflectively only through generated glue code,
# both ship their own consumer rules. Keep the Room generated implementations to be safe.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# kotlinx.serialization keeps the generated serializers of navigation routes.
-keepclassmembers class ** {
    public static ** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
