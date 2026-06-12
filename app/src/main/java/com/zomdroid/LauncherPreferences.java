package com.zomdroid;

public class LauncherPreferences {
    public enum Renderer {
        ZINK_ZFA("libzfa.so"),
        ZINK_OSMESA("libOSMesa.so"),
        GL4ES("libgl4es.so");

        public final String libName;

        Renderer(String libName) {
            this.libName = libName;
        }
    }

    public enum VulkanDriver {
        SYSTEM_DEFAULT(null),
        FREEDRENO("libvulkan_freedreno.so");

        public final String libName;

        VulkanDriver(String libName) {
            this.libName = libName;
        }
    }

    public enum AudioAPI {
        AAUDIO,
        OPENSL
    }
}
