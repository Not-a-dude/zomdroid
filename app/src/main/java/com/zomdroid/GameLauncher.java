package com.zomdroid;

import android.app.ActivityManager;
import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.view.Surface;

import com.zomdroid.data.GameSettings;
import com.zomdroid.game.GameInstance;

import java.util.ArrayList;

public class GameLauncher {
    public static void launch(GameInstance gameInstance, GameSettings settings) throws ErrnoException {

/*        // for debug
        Os.setenv("MESA_DEBUG", "1", false);
        Os.setenv("MESA_LOG_LEVEL", "debug", false);
        Os.setenv("ZINK_DEBUG", "validation", false);
        Os.setenv("mesa_glthread", "false", false);
        Os.setenv("GALLIUM_THREAD", "0", false);
        Os.setenv("VK_LOADER_DEBUG", "all", false);
        Os.setenv("VK_DEBUG", "all", false);
        Os.setenv("GALLIUM_DEBUG", "all", false);
        Os.setenv("VK_LOADER_LAYERS_ENABLE", "VK_LAYER_KHRONOS_validation", false);
        Os.setenv("BOX64_LOG", "3", false);
        Os.setenv("BOX64_DYNAREC", "0", false);*/

        //Os.setenv("LIBGL_NOERROR", "1", false);
/*        Os.setenv("LIBGL_LOGSHADERERROR", "1", false);
        Os.setenv("ZINK_DEBUG", "spirv", false);*/

        Os.setenv("LIBGL_MIPMAP", "1", false);

        Os.setenv("BOX64_LOG", "1", false);
        Os.setenv("BOX64_SHOWBT", "1", false);

        Os.setenv("BOX64_LD_LIBRARY_PATH", gameInstance.getLdLibraryPathForEmulation(), false);

        // Consumed by the zomdroid linker: dlopen of the desktop x86_64 libfmod.so/libfmodstudio.so
        // from the game files is redirected to the native arm64 fmod core libraries in this directory
        Os.setenv("ZOMDROID_FMOD_LIBRARY_DIR", AppStorage.requireSingleton().getHomePath() + "/"
                + gameInstance.getFmodLibraryPath(), false);

        Os.setenv("GALLIUM_DRIVER", "zink", false);

        Os.setenv("ZOMDROID_CACHE_DIR", AppStorage.requireSingleton().getCachePath(), false);

        Os.setenv("ZOMDROID_GLES_MAJOR", "2", false);
        Os.setenv("ZOMDROID_GLES_MINOR", "1", false);

        // for debugging GL calls, only supported on GL ES 3.2+ with GL_KHR_debug extension present
/*        Os.setenv("ZOMDROID_DEBUG_GL", "1", false);
        Os.setenv("LIBGL_GLES", "libGLESv3.so", false);
        Os.setenv("ZOMDROID_GLES_MAJOR", "3", true);
        Os.setenv("ZOMDROID_GLES_MINOR", "2", true);*/

        initZomdroidWindow(settings.getRenderer().name(),
                settings.getVulkanDriver().libName,
                settings.getAudioAPI().name());

        ArrayList<String> jvmArgs = gameInstance.getJvmArgsAsList();

        boolean hasXmx = false;
        for (String arg : jvmArgs) {
            if (arg.startsWith("-Xmx")) {
                hasXmx = true;
                break;
            }
        }
        if (!hasXmx) {
            long totalRamMb = getSystemTotalRamMb();
            long availRamMb = getSystemAvailableRamMb();

            long maxHeapMb = getMaxHeapMb(totalRamMb);

            Log.i("GameLauncher", "Calculated JVM Heap: totalRAM=" + totalRamMb + "MB, availRAM=" + availRamMb + "MB -> -Xmx" + maxHeapMb + "m");

            jvmArgs.add("-Xms1024m");
            jvmArgs.add("-Xmx" + maxHeapMb + "m");
            jvmArgs.add("-XX:InitiatingHeapOccupancyPercent=45");
            jvmArgs.add("-XX:+UnlockDiagnosticVMOptions");
            jvmArgs.add("-XX:+UseG1GC");
        }

        jvmArgs.add("-Dorg.lwjgl.opengl.libname=" + settings.getRenderer().libName);
        jvmArgs.add("-Dzomdroid.renderer=" + settings.getRenderer().name());
        //jvmArgs.add("-XX:+PrintFlagsFinal"); // for debugging
        jvmArgs.add("-XX:ErrorFile=/dev/stdout"); // print jvm crash report to stdout for now

        ArrayList<String> args = gameInstance.getArgsAsList();
/*        args.add("-debug");
        args.add("-debuglog=Shader");*/

        String javaHomePath = AppStorage.requireSingleton().getHomePath() + "/" + gameInstance.getJrePath();
        String ldLibraryPath = AppStorage.requireSingleton().getLibraryPath() + ":/system/lib64:"
                + javaHomePath + "/lib:" + javaHomePath + "/lib/server:" + gameInstance.getJavaLibraryPath()
                + ":" + gameInstance.getGamePath() + ":" + gameInstance.getGamePath() + "/natives"
                + ":" + gameInstance.getGamePath() + "/natives/android/arm64-v8a";
        Log.i("GameLauncher", "Launching " + gameInstance.getName()
                + "\n jvmArgs=" + jvmArgs
                + "\n ldLibraryPath=" + ldLibraryPath
                + "\n args=" + args);
        GameLauncher.startGame(gameInstance.getGamePath(), ldLibraryPath, jvmArgs.toArray(new String[0]),
                gameInstance.getMainClassName(), args.toArray(new String[0]));
    }

    private static long getMaxHeapMb(long totalRamMb) {
        if (totalRamMb >= 10240) {
            return 5120;
        } else if (totalRamMb >= 6500) {
            return 3328;
        } else {
            return 2560;
        }
    }

    private static long getSystemTotalRamMb() {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) AppStorage.requireSingleton()
                    .getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.getMemoryInfo(mi);
                return mi.totalMem / (1024 * 1024);
            }
        } catch (Exception e) {
            Log.w("GameLauncher", "Failed to query total RAM", e);
        }
        return -1;
    }

    private static long getSystemAvailableRamMb() {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) AppStorage.requireSingleton()
                    .getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.getMemoryInfo(mi);
                return mi.availMem / (1024 * 1024);
            }
        } catch (Exception e) {
            Log.w("GameLauncher", "Failed to query available RAM", e);
        }
        return -1;
    }


    public static native int initZomdroidWindow(String renderer, String vulkanDriver, String audioAPI);

    public static native void destroyZomdroidWindow();

    public static native int setSurface(Surface surface, int width, int height);

    public static native void destroySurface();

    static native void startGame(String gameDirPath, String libraryDirPath, String[] jvmArgs, String mainClassName, String[] args);
}
