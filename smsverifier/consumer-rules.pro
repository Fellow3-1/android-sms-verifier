# Preserve public API classes and callbacks for library consumers using R8 / ProGuard
-keep public class com.androidstudy.smsverifier.** {
    public protected *;
}
-keepclassmembers class * implements com.androidstudy.smsverifier.OnSmsCatchListener {
    public void onSmsCatch(...);
}
