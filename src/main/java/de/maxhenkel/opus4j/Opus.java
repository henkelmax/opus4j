package de.maxhenkel.opus4j;

import java.io.IOException;

class Opus {

    private static boolean loaded;
    private static Exception error;

    /**
     * Loads the native Opus library.
     * This method is called automatically when creating an {@link OpusEncoder} or {@link OpusDecoder}.
     * If the library is already loaded, this method does nothing.
     * If the library could not be loaded, consecutive calls to this method will throw the same exception without reattempting to load the native library.
     *
     * @throws UnknownPlatformException if the operating system is not supported
     * @throws IOException              if the native library could not be extracted
     */
    public static void load() throws UnknownPlatformException, IOException {
        if (loaded) {
            if (error != null) {
                if (error instanceof IOException) {
                    throw (IOException) error;
                } else if (error instanceof UnknownPlatformException) {
                    throw (UnknownPlatformException) error;
                }
                throw new RuntimeException(error);
            }
            return;
        }
        try {
            LibraryLoader.load("opus4j");
            loaded = true;
        } catch (UnknownPlatformException | IOException e) {
            error = e;
            throw e;
        }
    }

    /**
     * Checks if the native Opus library is loaded.
     *
     * @return true if the native library is loaded
     */
    public static boolean isLoaded() {
        return loaded && error == null;
    }

}
