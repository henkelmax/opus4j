package de.maxhenkel.opus4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

class LibraryLoader {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    private static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    private static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    private static boolean isLinux() {
        return OS_NAME.contains("nux");
    }

    private static String getPlatform() throws UnknownPlatformException {
        if (isWindows()) {
            return "windows";
        } else if (isMac()) {
            return "mac";
        } else if (isLinux()) {
            return "linux";
        } else {
            throw new UnknownPlatformException(String.format("Unknown operating system: %s", OS_NAME));
        }
    }

    private static String getDynamicLibraryExtension() throws UnknownPlatformException {
        if (isWindows()) {
            return "dll";
        } else if (isMac()) {
            return "dylib";
        } else if (isLinux()) {
            return "so";
        } else {
            throw new UnknownPlatformException(String.format("Unknown operating system: %s", OS_NAME));
        }
    }

    private static String getStaticLibraryExtension() {
        if (isWindows()) {
            return "lib";
        } else {
            return "a";
        }
    }

    private static String getLibraryExtension(boolean dynamic) throws UnknownPlatformException {
        return dynamic ? getDynamicLibraryExtension() : getStaticLibraryExtension();
    }

    private static String getNativeFolderName() throws UnknownPlatformException {
        return String.format("%s-%s", getPlatform(), OS_ARCH);
    }

    private static String getResourcePath(String libName, boolean dynamic) throws UnknownPlatformException {
        return String.format("natives/%s/%s.%s", getNativeFolderName(), libName, getLibraryExtension(dynamic));
    }

    public static void load(String libraryName, boolean dynamic) throws UnknownPlatformException, IOException {
        File tempFile = File.createTempFile(libraryName, String.format(".%s", getLibraryExtension(dynamic)));
        tempFile.deleteOnExit();

        try (InputStream in = LibraryLoader.class.getClassLoader().getResourceAsStream(getResourcePath(libraryName, dynamic))) {
            if (in == null) {
                throw new UnknownPlatformException(String.format("Could not find %s natives for platform %s", libraryName, getNativeFolderName()));
            }
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            System.load(tempFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            throw new UnknownPlatformException(String.format("Could not load %s natives for %s", libraryName, getNativeFolderName()), e);
        }
    }

    public static void load(String libraryName) throws IOException, UnknownPlatformException {
        load(libraryName, true);
    }

}
