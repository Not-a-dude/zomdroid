package com.zomdroid.game;

import com.zomdroid.AppStorage;
import com.zomdroid.C;
import com.zomdroid.FileUtils;

import java.io.File;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;

public class GameInstance {
    private static final String INSTANCES_ROOT_DIR_NAME = "instances";
    public static final String GAME_FILES_DIR_NAME = "game";

    private String name;
    private String homePath;
    private boolean installationFinished = false;
    private String[] classPath;
    private String[] extraClassPath;
    private String[] libraryPath;
    private String[] libraryPathForEmulation;
    private String fmodLibraryPath;
    private String[] extraJvmArgs;
    private String[] args;
    private String mainClassName;
    private String javaAgentPath;
    private String javaAgentArgs;
    private int jreVersion;

    public GameInstance(String name, InstallationPreset preset) throws FileSystemException {
        this.name = name;
        makeDirs();
        this.classPath = preset.classPathArray;
        this.extraClassPath = preset.extraJars;
        this.libraryPath = preset.libraryPathArray;
        this.libraryPathForEmulation = preset.libraryPathForEmulationArray;
        this.fmodLibraryPath = preset.fmodLibraryPath;
        this.extraJvmArgs = preset.extraJvmArgs;
        this.args = preset.args;
        this.mainClassName = preset.mainClassName;
        this.javaAgentPath = preset.javaAgentPath;
        this.javaAgentArgs = preset.javaAgentArgs;
        this.jreVersion = preset.jreVersion;
    }

    private static String buildHomePath(String name) {
        return AppStorage.requireSingleton().getHomePath() + "/" + INSTANCES_ROOT_DIR_NAME + "/" + name;
    }

    public static boolean isValidName(String name) {
        return FileUtils.isValidFilenameStrict(name);
    }

    public static boolean isUniqueName(String name) {
        return !new File(buildHomePath(name)).exists();
    }

    public String getName() {
        return this.name;
    }

    public String getHomePath() {
        return this.homePath;
    }

    public String getGamePath() {
        return this.homePath + "/" + GAME_FILES_DIR_NAME;
    }

    public String getLdLibraryPathForEmulation() {
        StringJoiner joiner = new StringJoiner(":");
        for (String path : this.libraryPathForEmulation) {
            joiner.add(AppStorage.requireSingleton().getHomePath() + "/" + path);
        }
        joiner.add(getGamePath());
        joiner.add(getGamePath() + "/natives");
        return joiner + ":.";
    }

    public String getFmodLibraryPath() {
        return fmodLibraryPath;
    }

    public String getJrePath() {
        if (jreVersion == 25) {
            return C.deps.JRE_25;
        }
        return C.deps.JRE_17;
    }

    public String getJavaLibraryPath() {
        StringJoiner libsJoiner = new StringJoiner(":");
        for (String path : this.libraryPath) {
            libsJoiner.add(AppStorage.requireSingleton().getHomePath() + "/" + path);
        }
        return libsJoiner.toString();
    }

    public ArrayList<String> getJvmArgsAsList() {
        ArrayList<String> jvmArgsList = new ArrayList<>();
        jvmArgsList.add("-Duser.home=" + this.homePath);
        jvmArgsList.add("-Djava.io.tmpdir=" + AppStorage.requireSingleton().getCachePath());

        // arm64 game natives take priority
        jvmArgsList.add("-Djava.library.path=" + getJavaLibraryPath() + ":.:./natives/android/arm64-v8a:./natives");

        //jvmArgsList.add("-Dorg.lwjgl.util.Debug=true"); // debug

        StringJoiner jarsJoiner = new StringJoiner(":");
        for (String path : this.extraClassPath) {
            jarsJoiner.add(AppStorage.requireSingleton().getHomePath() + "/" + path);
        }
        jvmArgsList.add("-Djava.class.path=" + String.join(":", this.classPath) + ":" + jarsJoiner);

        for (String extraArg : this.extraJvmArgs) {
            if ("-XX:+UseZGC".equals(extraArg)) {
                jvmArgsList.add("-XX:+UseG1GC");
            } else {
                jvmArgsList.add(extraArg);
            }
        }

        if (!this.javaAgentPath.isEmpty()) {
            StringBuilder agentBuilder = new StringBuilder();
            agentBuilder.append("-javaagent:").append(AppStorage.requireSingleton().getHomePath() + "/" + this.javaAgentPath);
            if (!this.javaAgentArgs.isEmpty()) {
                agentBuilder.append("=").append(this.javaAgentArgs);
            }
            jvmArgsList.add(agentBuilder.toString());
        }

        return jvmArgsList;
    }

    public ArrayList<String> getArgsAsList() {
        return new ArrayList<>(Arrays.asList(this.args));
    }

    public String getMainClassName() {
        return this.mainClassName;
    }

    private void makeDirs() throws FileSystemException {
        File homeDir = new File(buildHomePath(this.name));
        if (!homeDir.mkdirs()) {
            throw new FileSystemException("Failed to create instance directory " + homeDir.getAbsolutePath());
        }
        this.homePath = homeDir.getAbsolutePath();

        File gameDir = new File(getGamePath());
        if (!gameDir.mkdirs()) {
            throw new FileSystemException("Failed to create game files directory " + gameDir.getAbsolutePath());
        }
    }

    public boolean isInstallationFinished() {
        return this.installationFinished;
    }

    protected void markInstallationFinished() {
        this.installationFinished = true;
    }

    public boolean hasGameFiles() {
        File mainClassFile = new File(getGamePath() + "/" + getMainClassName() + ".class");
        if (mainClassFile.exists()) return true;

        File mainJarFile = new File(getGamePath() + "/projectzomboid.jar");
        return mainJarFile.exists();
    }

    public boolean hasFilesForLinux() {
        File pzBulletFile = new File(getGamePath() + "/libPZBullet64.so");
        if (pzBulletFile.exists()) return true;

        File pzBulletNativesFile = new File(getGamePath() + "/natives/libPZBullet64.so");
        return pzBulletNativesFile.exists();
    }
}
