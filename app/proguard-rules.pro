# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Data Entities and Enum classes used in Room / Serialization
-keep class com.screentask.core.data.local.entity.** { *; }
-keep class com.screentask.core.classifier.model.** { *; }

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Google Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**