# Android Gradle Plugin, AndroidX, Room, Health Connect, coroutines, and Google Play
# services provide their own consumer rules. App classes are referenced directly or
# declared in the manifest, so broad application/framework keep rules are unnecessary.

# Preserve generic signatures and runtime annotations used by Android libraries without
# preventing class/member shrinking or obfuscation.
-keepattributes Signature,*Annotation*

# Release-only diagnostic logging has no observable side effects.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
