# MedMate Proguard / R8 Optimization Rules

# Keep Firestore Models and their properties from name mangling
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.example.mayuranddhyeyinternshipproject.data.model.** { *; }

# Keep Android Jetpack Security Crypto
-keepclassmembers class * extends androidx.security.crypto.MasterKey { *; }
-keep class androidx.security.crypto.** { *; }

# Keep Firebase Firestore reflection
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.ServerTimestamp <methods>;
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
    @com.google.firebase.firestore.IgnoreExtraProperties <methods>;
}

# Keep Coroutines and Kotlin serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
