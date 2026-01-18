# Add project specific ProGuard rules here.

# Keep all classes in the app package
-keep class com.oceanofmaya.intervalwalktrainer.** { *; }

# Keep Kotlin data classes (used for state management)
-keepclassmembers class com.oceanofmaya.intervalwalktrainer.** {
    <fields>;
}

# Keep sealed classes
-keep class com.oceanofmaya.intervalwalktrainer.IntervalPhase { *; }
-keep class com.oceanofmaya.intervalwalktrainer.IntervalPhase$* { *; }

# Keep data classes used for state
-keep class com.oceanofmaya.intervalwalktrainer.TimerState { *; }
-keep class com.oceanofmaya.intervalwalktrainer.IntervalFormula { *; }

# Keep object classes (singletons)
-keep class com.oceanofmaya.intervalwalktrainer.IntervalFormulas { *; }

# Text-to-Speech classes (used via reflection)
-keep class android.speech.tts.** { *; }
-keep interface android.speech.tts.** { *; }
-dontwarn android.speech.tts.**

# Keep TTS callback interfaces
-keep class * implements android.speech.tts.TextToSpeech$OnInitListener {
    <init>(...);
}

# Keep classes used by Android framework
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Keep ViewBinding generated classes
-keep class * extends androidx.viewbinding.ViewBinding {
    public static * inflate(...);
    public static * bind(...);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release (optional - uncomment if you want to remove Log calls)
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
#     public static *** w(...);
#     public static *** e(...);
# }
