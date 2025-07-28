package de.maxhenkel.opus4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class OpusEncoderTest {

    @Test
    @DisplayName("Encode")
    void encode() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            byte[] encoded1 = encoder.encode(new short[120]);
            assertTrue(encoded1.length > 0);
            byte[] encoded2 = encoder.encode(new short[240]);
            assertTrue(encoded2.length > 0);
            byte[] encoded3 = encoder.encode(new short[480]);
            assertTrue(encoded3.length > 0);
            byte[] encoded4 = encoder.encode(new short[960]);
            assertTrue(encoded4.length > 0);
            byte[] encoded5 = encoder.encode(new short[1920]);
            assertTrue(encoded5.length > 0);
            byte[] encoded6 = encoder.encode(new short[2880]);
            assertTrue(encoded6.length > 0);
        }
    }

    @Test
    @DisplayName("Invalid encoding")
    void invalidEncoding() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            IOException e1 = assertThrowsExactly(IOException.class, () -> {
                encoder.encode(new short[0]);
            });
            assertEquals("Failed to encode: OPUS_BAD_ARG", e1.getMessage());

            IOException e2 = assertThrowsExactly(IOException.class, () -> {
                encoder.encode(new short[1]);
            });
            assertEquals("Failed to encode: OPUS_BAD_ARG", e2.getMessage());

            IOException e3 = assertThrowsExactly(IOException.class, () -> {
                encoder.encode(new short[239]);
            });
            assertEquals("Failed to encode: OPUS_BAD_ARG", e3.getMessage());

            IOException e4 = assertThrowsExactly(IOException.class, () -> {
                encoder.encode(new short[961]);
            });
            assertEquals("Failed to encode: OPUS_BAD_ARG", e4.getMessage());
        }
    }

    @Test
    @DisplayName("Encode with frame size")
    void encodeWithFrameSize() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            encoder.setMaxPayloadSize(1);
            byte[] encoded = encoder.encode(new short[960]);
            assertEquals(1, encoded.length);
            encoder.setMaxPayloadSize(Integer.MAX_VALUE);
            byte[] encoded2 = encoder.encode(new short[960]);
            assertTrue(encoded2.length > 0);
        }
    }

    @Test
    @DisplayName("Reset state")
    void resetState() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            encoder.encode(new short[960]);
            encoder.resetState();
            encoder.encode(new short[960]);
        }
    }

    @Test
    @DisplayName("Invalid channel count")
    void invalidChannels() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            OpusEncoder encoder = new OpusEncoder(48000, 3, OpusEncoder.Application.VOIP);
            encoder.close();
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            OpusEncoder encoder = new OpusEncoder(48000, 0, OpusEncoder.Application.VOIP);
            encoder.close();
        });
    }

    @Test
    @DisplayName("Double close")
    void doubleClose() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        encoder.encode(new short[960]);
        encoder.close();
        encoder.close();
    }

    @Test
    @DisplayName("Encode after close")
    void encodeAfterClose() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        encoder.encode(new short[960]);
        encoder.close();
        RuntimeException e = assertThrowsExactly(RuntimeException.class, () -> {
            encoder.encode(new short[960]);
        });
        assertEquals("Encoder is closed", e.getMessage());
    }

    @Test
    @DisplayName("Invalid sample rate")
    void invalidSampleRate() {
        IOException e = assertThrowsExactly(IOException.class, () -> {
            OpusEncoder encoder = new OpusEncoder(48001, 1, OpusEncoder.Application.VOIP);
            encoder.close();
        });
        assertEquals("Failed to create encoder: OPUS_BAD_ARG", e.getMessage());
    }

    @Test
    @DisplayName("Valid sample rates")
    void validSampleRates() throws IOException, UnknownPlatformException {
        new OpusEncoder(8000, 1, OpusEncoder.Application.VOIP).close();
        new OpusEncoder(12000, 1, OpusEncoder.Application.VOIP).close();
        new OpusEncoder(16000, 1, OpusEncoder.Application.VOIP).close();
        new OpusEncoder(24000, 1, OpusEncoder.Application.VOIP).close();
        new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP).close();
    }

    @Test
    @DisplayName("Get Opus version")
    void getOpusVersion() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        assertEquals("libopus", encoder.getOpusVersion().split(" ")[0]);
        assertTrue(encoder.getOpusVersion().matches("libopus \\d+\\.\\d+\\.\\d+"));
        encoder.close();
    }

    @Test
    @DisplayName("Invalid maximum payload size")
    void invalidMaximumPayloadSize() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            IllegalArgumentException e1 = assertThrowsExactly(IllegalArgumentException.class, () -> {
                encoder.setMaxPayloadSize(0);
            });
            assertEquals("Invalid maximum payload size: 0", e1.getMessage());
            IllegalArgumentException e2 = assertThrowsExactly(IllegalArgumentException.class, () -> {
                encoder.setMaxPayloadSize(-1);
            });
            assertEquals("Invalid maximum payload size: -1", e2.getMessage());
            IllegalArgumentException e3 = assertThrowsExactly(IllegalArgumentException.class, () -> {
                encoder.setMaxPayloadSize(Integer.MIN_VALUE);
            });
            assertEquals("Invalid maximum payload size: " + Integer.MIN_VALUE, e3.getMessage());
        }
    }

    @Test
    @DisplayName("Get payload size")
    void getPayloadSize() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            encoder.setMaxPayloadSize(1);
            assertEquals(1, encoder.getMaxPayloadSize());
            encoder.setMaxPayloadSize(128);
            assertEquals(128, encoder.getMaxPayloadSize());
            encoder.setMaxPayloadSize(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, encoder.getMaxPayloadSize());
        }
    }

}
