package de.maxhenkel.opus4j;

import javax.annotation.Nullable;
import java.io.IOException;

public class OpusDecoder implements AutoCloseable {

    private long decoder;

    /**
     * Creates a new Opus decoder.
     *
     * @param sampleRate the sample rate (8000, 12000, 16000, 24000, or 48000)
     * @param channels   the number of channels (1 or 2)
     * @throws UnknownPlatformException if the operating system is not supported
     * @throws IOException              if the native library could not be extracted
     */
    public OpusDecoder(int sampleRate, int channels) throws IOException, UnknownPlatformException {
        Opus.load();
        decoder = createDecoder0(sampleRate, channels);
    }

    private static native String getOpusVersion0();

    public String getOpusVersion() {
        synchronized (this) {
            return getOpusVersion0();
        }
    }

    private static native long createDecoder0(int sampleRate, int channels) throws IOException;

    private native void setFrameSize0(int frameSize);

    public void setFrameSize(int frameSize) {
        synchronized (this) {
            setFrameSize0(frameSize);
        }
    }

    private native int getFrameSize0();

    public int getFrameSize() {
        synchronized (this) {
            return getFrameSize0();
        }
    }

    private native short[] decode0(@Nullable byte[] input, boolean fec);

    public short[] decode(@Nullable byte[] input, boolean fec) {
        synchronized (this) {
            return decode0(input, fec);
        }
    }

    public short[] decode(@Nullable byte[] input) {
        return decode(input, false);
    }

    public short[] decodeFec() {
        return decode(null, true);
    }

    private native void resetState0();

    public void resetState() {
        synchronized (this) {
            resetState0();
        }
    }

    private native void destroyDecoder0();

    @Override
    public void close() {
        synchronized (this) {
            destroyDecoder0();
            decoder = 0L;
        }
    }

    public boolean isClosed() {
        synchronized (this) {
            return decoder == 0L;
        }
    }

    @Override
    public String toString() {
        synchronized (this) {
            return String.format("OpusDecoder[%d]", decoder);
        }
    }
}
