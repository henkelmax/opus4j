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
        decoder = createDecoder(sampleRate, channels);
    }

    private static native long createDecoder(int sampleRate, int channels) throws IOException;

    public native void setFrameSize(int frameSize);

    public native int getFrameSize();

    public native short[] decode(@Nullable byte[] input, boolean fec);

    public short[] decode(@Nullable byte[] input) {
        return decode(input, false);
    }

    public short[] decodeFec() {
        return decode(null, true);
    }

    public native void resetState();

    private native void destroyDecoder();

    @Override
    public void close() {
        destroyDecoder();
        decoder = 0L;
    }

    public boolean isClosed() {
        return decoder == 0L;
    }

    @Override
    public String toString() {
        return String.format("OpusDecoder[%d]", decoder);
    }
}
