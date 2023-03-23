package de.maxhenkel.opus4j;

import java.io.IOException;

public class OpusEncoder implements AutoCloseable {

    private long encoder;

    /**
     * Creates a new Opus encoder.
     *
     * @param sampleRate  the sample rate (8000, 12000, 16000, 24000, or 48000)
     * @param channels    the number of channels (1 or 2)
     * @param application the application (VOIP, AUDIO, or LOW_DELAY)
     * @throws UnknownPlatformException if the operating system is not supported
     * @throws IOException              if the native library could not be extracted
     */
    public OpusEncoder(int sampleRate, int channels, Application application) throws IOException, UnknownPlatformException {
        Opus.load();
        encoder = createEncoder(sampleRate, channels, application);
    }

    private static native long createEncoder(int sampleRate, int channels, Application application) throws IOException;

    public native void setMaxPayloadSize(int maxPayloadSize);

    public native int getMaxPayloadSize();

    public native byte[] encode(short[] input);

    public native void resetState();

    private native void destroyEncoder();

    @Override
    public void close() {
        destroyEncoder();
        encoder = 0L;
    }

    public boolean isClosed() {
        return encoder == 0L;
    }

    @Override
    public String toString() {
        return String.format("OpusEncoder[%d]", encoder);
    }

    public static enum Application {
        VOIP(0),
        AUDIO(1),
        LOW_DELAY(2);

        private final int value;

        Application(int value) {
            this.value = value;
        }
    }

}
