# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/oday/Software/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:


######## Somewhat Global

-keepattributes *Annotation*
-keepattributes Signature

######## Butterknife

-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}


######## Glide

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}


######## SOMETHING

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}


######## SOMETHING

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}


######## Android design support library

-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }


######## Otto

-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}


######## OkHttp

-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**


######## Amazon AWS

-keep class com.amazonaws.*


######## Parse

-keep class com.parse.** { *; }
-dontwarn android.net.SSLCertificateSocketFactory
-dontwarn android.app.Notification
-dontwarn okio.**

######## Fix: Unable to compute hash for classes.jar

#-dontwarn java.nio.file.Files
#-dontwarn java.nio.file.Path
#-dontwarn java.nio.file.OpenOption
#-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement


######## Crashlytics

-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**
-keepattributes SourceFile,LineNumberTable


######## Apache Harmony, Sun Mail and Java CommandInfo

-dontwarn java.awt.**, javax.security.**, java.beans.**


######## Amazon AWS
-keep class org.apache.commons.logging.**               { *; }
-keep class com.amazonaws.services.sqs.QueueUrlHandler  { *; }
-keep class com.amazonaws.javax.xml.transform.sax.*     { public *; }
-keep class com.amazonaws.javax.xml.stream.**           { *; }
-keep class com.amazonaws.services.**.model.*Exception* { *; }
-keep class org.codehaus.**                             { *; }
 
-dontwarn javax.xml.stream.events.**
-dontwarn org.codehaus.jackson.**
-dontwarn org.apache.commons.logging.impl.**
-dontwarn org.apache.http.conn.scheme.**
-dontwarn org.apache.http.annotation.**
-dontwarn com.amazonaws.**


######## ORM Lite

-keepclassmembers class * { 
  public <init>(android.content.Context); 
}
-keep class com.j256.** {
   *;
}
-keep class com.j256.**
-keepclassmembers class com.j256.**
-keep enum com.j256.**
-keepclassmembers enum com.j256.**
-keep interface com.j256.**
-keepclassmembers interface com.j256.**
-keep class com.om.snipit.models.**
-keepclassmembers class com.om.snipit.models.** { *; } 

######## RxJava

-keep class rx.** { * ; }

-dontwarn rx.internal.util.unsafe.*