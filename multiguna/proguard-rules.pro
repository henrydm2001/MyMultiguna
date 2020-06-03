# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# -keep class infix.imrankst1221.webapp.** { *; }

#-keep public class <infix.imrankst1221.webapp.ui.splash.ThemeActivity>
#-keep public class <infix.imrankst1221.webapp.ui.home.HomeActivity>
#-keep public class <infix.imrankst1221.webapp.MainActivity>

-keepattributes Signature -keep class com.firebase.** { *; }
-keep class org.apache.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.** -dontwarn org.w3c.dom.**
-dontwarn android.support.v4.**
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-keep class * extends com.myCompany.package.flavor.Flavor { *; }
-keep class com.myCompany.** { *; }
