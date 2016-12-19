# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

## --------------- Start Project specifics --------------- ##
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontobfuscate
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

-keepattributes Signature,*Annotation*,EnclosingMethod

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}
# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep the BuildConfig
-keep class dev.dworks.apps.anexplorer.BuildConfig { *; }

# Keep the support library
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

-dontwarn android.support.**

# Crashlytics 1.+
-keep class io.fabric.** { *; }
-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**
-keepattributes SourceFile,LineNumberTable

-keep class org.slf4j.** { *; }
-keepclassmembers class * implements org.apache.mina.core.service.IoProcessor {
    public <init>(java.util.concurrent.ExecutorService);
    public <init>(java.util.concurrent.Executor);
    public <init>();
}
-dontwarn org.apache.**
-dontwarn org.slf4j.**

# Application classes that will be serialized/deserialized over Gson
# or have been blown up by ProGuard in the past

## ---------------- End Project specifics ---------------- ##