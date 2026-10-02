package com.zomdroid;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class AppStorage {
    private final Context applicationContext;
    private final String HOME_DIR_PATH;
    private final String CACHE_DIR_PATH;
    private final String LIBRARY_DIR_PATH;
    private static AppStorage singleton;

    private AppStorage(Context applicationContext) {
        this.applicationContext = applicationContext;
        HOME_DIR_PATH = applicationContext.getFilesDir().getAbsolutePath();
        CACHE_DIR_PATH = applicationContext.getCacheDir().getAbsolutePath();
        LIBRARY_DIR_PATH = applicationContext.getApplicationInfo().nativeLibraryDir;
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public static void init(Context applicationContext) {
        singleton = new AppStorage(applicationContext);
    }

    @Nullable
    public static AppStorage getSingleton() {
        return singleton;
    }

    @NonNull
    public static AppStorage requireSingleton() {
        if (singleton == null) {
            throw new RuntimeException("AppStorage is not initialized");
        }
        return singleton;
    }

    public String getHomePath() {
        return HOME_DIR_PATH;
    }

    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public String getCachePath() {
        return CACHE_DIR_PATH;
    }

    public String getLibraryPath() {
        return LIBRARY_DIR_PATH;
    }
}
