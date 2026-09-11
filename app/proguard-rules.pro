# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/vongvireaksatya.bou/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep the entry point of the app
-keep class com.example.sealyoutube.MainActivity { *; }

# Obfuscate all other classes
-repackageclasses ''
-allowaccessmodification

# Remove Log statements in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Preserve line numbers and source file names for crash reporting (optional, remove for max security)
#-keepattributes SourceFile,LineNumberTable
