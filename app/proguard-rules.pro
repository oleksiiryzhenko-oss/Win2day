# Room, DataStore and Compose ship their own consumer rules.
# Keep Room entities' field names stable for readability in crash reports.
-keep class com.taichiflow.app.data.local.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod
