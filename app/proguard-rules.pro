-keep class org.matrix.chromext.MainHook {
 <init>();
}
-keepattributes SourceFile,LineNumberTable

# Keep every first-party name (package, class, method and field) readable in
# release stack traces, while still allowing R8 to remove unused code. Line
# numbers are kept above so traces stay readable without retracing.
-keep,allowshrinking class org.matrix.chromext.** { *; }

# Room instantiates the generated database implementation by name.
-keep class org.matrix.chromext.db.ChromeXtDatabase_Impl {
    <init>();
}

# Parasitics injects module activities into the host process and creates them
# with Class.newInstance(), so keep every Activity subclass, its no-arg
# constructor and lifecycle methods instead of listing individual classes.
-keep class * extends android.app.Activity { *; }

-keepclassmembers class io.github.qauxv.lifecycle.Parasitics {
    boolean __stub_hooked;
}