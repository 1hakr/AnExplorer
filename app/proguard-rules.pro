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
-dontobfuscate
-dontwarn
-optimizationpasses 5

#When not preverifing in a case-insensitive filing system, such as Windows. Because this tool unpacks your processed jars, you should then use:
-dontusemixedcaseclassnames

#Specifies not to ignore non-public library classes. As of version 4.5, this is the default setting
-dontskipnonpubliclibraryclasses

#Preverification is irrelevant for the dex compiler and the Dalvik VM, so we can switch it off with the -dontpreverify option.
-dontpreverify

#Specifies to write out some more information during processing. If the program terminates with an exception, this option will print out the entire stack trace, instead of just the exception message.
-verbose

#The -optimizations option disables some arithmetic simplifications that Dalvik 1.0 and 1.5 can't handle. Note that the Dalvik VM also can't handle aggressive overloading (of static fields).
#To understand or change this check http://proguard.sourceforge.net/index.html#/manual/optimizations.html
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# For stack traces
-keepattributes *Annotation*, SourceFile, LineNumberTable

# Get rid of package names, makes file smaller
#-repackageclasses

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

-keepclassmembers class * implements android.os.Parcelable {
 public <fields>;
}

# Keep the BuildConfig
-keep class dev.dworks.apps.anexplorer.BuildConfig { *; }

# Keep the support library
-keep public class androidx.appcompat.widget.SearchView { *; }

-keepclassmembers public class androidx.recyclerview.widget.RecyclerView { *; }

-keep public class * extends androidx.core.view.ActionProvider {
    public <init>(android.content.Context);
}

-keep class org.slf4j.** { *; }
-keepclassmembers class * implements org.apache.mina.core.service.IoProcessor {
    public <init>(java.util.concurrent.ExecutorService);
    public <init>(java.util.concurrent.Executor);
    public <init>();
}

# Required for CloudRail
-keep class com.cloudrail.** { *; }

# Required for Retrofit/OkHttp
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keepattributes *Annotation*, Signature, Exceptions

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.core.**
-dontwarn org.apache.mina.proxy.**
-dontwarn org.apache.mina.util.**
-keep class java.util.concurrent.ConcurrentHashMap { *; }



-dontwarn org.slf4j.**

-dontwarn dagger.releasablereferences.*

# Application classes that will be serialized/deserialized over Gson
# or have been blown up by ProGuard in the past

## ---------------- End Project specifics ---------------- ##