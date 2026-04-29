# ============================================================================================
# БАЗОВЫЕ ПРАВИЛА
# ============================================================================================

-dontwarn java.awt.**
-dontwarn org.apache.poi.**
-dontwarn javax.xml.**

-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*
-renamesourcefileattribute SourceFile

# ============================================================================================
# ANDROID & JETPACK
# ============================================================================================

-keep class androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.navigation.NavArgs

# ============================================================================================
# HILT & DI
# ============================================================================================

-keep class dagger.hilt.internal.aggregatedroot.** { *; }
-keepclassmembers class * { @javax.inject.Inject <init>(...); }
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }

# ============================================================================================
# ROOM
# ============================================================================================

-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * { @androidx.room.* *; }

# ============================================================================================
# FIREBASE & GMS
# ============================================================================================

-keepnames class com.google.android.gms.** { *; }
-keepnames class com.google.firebase.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.messaging.** { *; }

# ============================================================================================
# ML KIT & CAMERA
# ============================================================================================

-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ============================================================================================
# EXCEL — Apache POI
# Полный набор правил для работы POI с R8/minify
# ============================================================================================

-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.w3c.dom.** { *; }
-keep class javax.xml.** { *; }
-keep class javax.xml.stream.** { *; }
-keep class org.dhatim.fastexcel.** { *; }
-keep class org.codehaus.woodstox.** { *; }
-keep class com.fasterxml.aalto.** { *; }
-keep class org.apache.xmlbeans.** { *; }

# ServiceLoader — POI регистрирует провайдеры через него
-keepnames class * implements org.apache.poi.ss.usermodel.Workbook
-keepnames class * implements java.util.Iterator

# LambdaMetafactory — POI использует через reflection
-keep class java.lang.invoke.** { *; }
-keepclassmembers class * {
    java.lang.invoke.MethodHandle *;
}

-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn com.microsoft.schemas.**
-dontwarn java.lang.invoke.**
-dontwarn org.apache.xmlbeans.**

# ============================================================================================
# COIL & GSON
# ============================================================================================

-keep class coil.** { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================================
# ТВОИ КЛАССЫ (критично для Firestore data классов)
# ============================================================================================

-keep class com.example.qrscannerapp.**.model.** { *; }
-keep class com.example.qrscannerapp.**.data.** { *; }

-keep class com.example.qrscannerapp.StorageCell { *; }
-keep class com.example.qrscannerapp.CellOperation { *; }
-keep class com.example.qrscannerapp.StorageActivityLogEntry { *; }
-keep class com.example.qrscannerapp.features.inventory.domain.model.StoragePallet { *; }
-keep class com.example.qrscannerapp.features.inventory.domain.model.PalletActivityLogEntry { *; }

-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers @androidx.annotation.Keep class * { *; }

# ============================================================================================
# SECURITY GUARD
# ============================================================================================

-keep class com.example.qrscannerapp.SecurityPreferences { *; }
-keepclassmembers class com.example.qrscannerapp.AppSecurityGuard {
    @javax.inject.Inject <init>(...);
    public *** runSecurityCheckAsync();
    public *** runQuickCheck();
    public *** isDeviceSafe();
    public *** getApkSignatureHash();
}

-keepclassmembers class com.example.qrscannerapp.TelemetryManager {
    @javax.inject.Inject <init>(...);
}

-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ============================================================================================
# АГРЕССИВНАЯ ОБФУСКАЦИЯ (защита от реверс-инжиниринга)
# ============================================================================================

-repackageclasses ''
-allowaccessmodification
-overloadaggressively