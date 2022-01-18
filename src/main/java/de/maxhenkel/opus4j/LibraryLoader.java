package de.maxhenkel.opus4j;

import com.sun.jna.Platform;

class LibraryLoader {
    private static String nativesPath = "";

    public static String setNativesPath(String path) {
        nativesPath = path;
    }

    public static String getPath() {
        String platform = Platform.RESOURCE_PREFIX;
        return String.format("%s/natives/%s/libopus.%s", nativesPath, platform, getExtension(platform));
    }

    private static String getExtension(String platform) {
        switch (platform) {
            case "darwin":
                return "dylib";
            case "win32-x86":
            case "win32-x86-64":
                return "dll";
            case "linux-arm":
            case "linux-aarch64":
            case "linux-x86":
            case "linux-x86-64":
            default:
                return "so";
        }
    }

}
