package de.maxhenkel.opus4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class OpusDecoderTest {

    @Test
    @DisplayName("Encode")
    void encode() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        byte[] encoded = encoder.encode(new short[960]);

        OpusDecoder decoder = new OpusDecoder(48000, 1);
        short[] decoded = decoder.decode(encoded);
        decoder.close();
        assertEquals(960, decoded.length);
    }

    @Test
    @DisplayName("Invalid channel count")
    void invalidChannels() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            OpusDecoder decoder = new OpusDecoder(48000, 3);
            decoder.close();
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            OpusDecoder encoder = new OpusDecoder(48000, 0);
            encoder.close();
        });
    }

    @Test
    @DisplayName("Decode after close")
    void encodeAfterClose() throws IOException, UnknownPlatformException {
        OpusDecoder decoder = new OpusDecoder(48000, 1);
        decoder.decode(null);
        decoder.close();
        RuntimeException e = assertThrowsExactly(RuntimeException.class, () -> {
            decoder.decode(null);
        });
        assertEquals("Decoder is closed", e.getMessage());
    }

    @Test
    @DisplayName("Invalid sample rate")
    void invalidSampleRate() {
        IOException e = assertThrowsExactly(IOException.class, () -> {
            OpusDecoder decoder = new OpusDecoder(48001, 1);
            decoder.close();
        });
        assertEquals("Failed to create decoder: OPUS_BAD_ARG", e.getMessage());
    }

    @Test
    @DisplayName("Valid sample rates")
    void validSampleRates() throws IOException, UnknownPlatformException {
        new OpusDecoder(8000, 1).close();
        new OpusDecoder(12000, 1).close();
        new OpusDecoder(16000, 1).close();
        new OpusDecoder(24000, 1).close();
        new OpusDecoder(48000, 1).close();
    }

    @Test
    @DisplayName("Get Opus version")
    void getOpusVersion() throws IOException, UnknownPlatformException {
        OpusDecoder decoder = new OpusDecoder(48000, 1);
        assertEquals("libopus", decoder.getOpusVersion().split(" ")[0]);
        assertTrue(decoder.getOpusVersion().matches("libopus \\d+\\.\\d+\\.\\d+"));
        decoder.close();
    }

}
