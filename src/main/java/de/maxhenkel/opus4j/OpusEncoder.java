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
        encoder = createEncoder0(sampleRate, channels, application);
    }

    private static native long createEncoder0(int sampleRate, int channels, Application application) throws IOException;

    private native void setMaxPayloadSize0(int maxPayloadSize);

    public void setMaxPayloadSize(int maxPayloadSize) {
        synchronized (this) {
            setMaxPayloadSize0(maxPayloadSize);
        }
    }

    private native int getMaxPayloadSize0();

    public int getMaxPayloadSize() {
        synchronized (this) {
            return getMaxPayloadSize0();
        }
    }

    private native byte[] encode0(short[] input);

    public byte[] encode(short[] input) {
        synchronized (this) {
            return encode0(input);
        }
    }

    private native void resetState0();

    public void resetState() {
        synchronized (this) {
            resetState0();
        }
    }

    private native void destroyEncoder0();

    @Override
    public void close() {
        synchronized (this) {
            destroyEncoder0();
            encoder = 0L;
        }
    }

    public boolean isClosed() {
        synchronized (this) {
            return encoder == 0L;
        }
    }

    @Override
    public String toString() {
        synchronized (this) {
            return String.format("OpusEncoder[%d]", encoder);
        }
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
