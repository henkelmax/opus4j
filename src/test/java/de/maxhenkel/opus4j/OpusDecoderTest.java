package de.maxhenkel.opus4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class OpusDecoderTest {

    @Test
    @DisplayName("Decode")
    void decode() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            byte[] encoded1 = encoder.encode(new short[120]);
            byte[] encoded2 = encoder.encode(new short[240]);
            byte[] encoded3 = encoder.encode(new short[480]);
            byte[] encoded4 = encoder.encode(new short[960]);
            byte[] encoded5 = encoder.encode(new short[1920]);
            byte[] encoded6 = encoder.encode(new short[2880]);

            try (OpusDecoder decoder = new OpusDecoder(48000, 1)) {
                decoder.setFrameSize(2880);
                short[] decoded1 = decoder.decode(encoded1);
                assertEquals(120, decoded1.length);
                short[] decoded2 = decoder.decode(encoded2);
                assertEquals(240, decoded2.length);
                short[] decoded3 = decoder.decode(encoded3);
                assertEquals(480, decoded3.length);
                short[] decoded4 = decoder.decode(encoded4);
                assertEquals(960, decoded4.length);
                short[] decoded5 = decoder.decode(encoded5);
                assertEquals(1920, decoded5.length);
                short[] decoded6 = decoder.decode(encoded6);
                assertEquals(2880, decoded6.length);
            }
        }
    }

    @Test
    @DisplayName("Decode invalid packet")
    void decodeInvalidFrameSize() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            byte[] encoded = encoder.encode(new short[960]);
            try (OpusDecoder decoder = new OpusDecoder(48000, 1)) {
                byte[] cutoff = new byte[encoded.length - 4];
                System.arraycopy(encoded, 4, cutoff, 0, cutoff.length);
                IOException e1 = assertThrowsExactly(IOException.class, () -> {
                    decoder.decode(cutoff);
                });
                assertEquals("Failed to decode: OPUS_INVALID_PACKET", e1.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Wrong frame size")
    void wrongFrameSize() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            byte[] encoded1 = encoder.encode(new short[120]);
            byte[] encoded2 = encoder.encode(new short[2880]);

            try (OpusDecoder decoder = new OpusDecoder(48000, 1)) {
                decoder.setFrameSize(119);
                IOException e1 = assertThrowsExactly(IOException.class, () -> {
                    decoder.decode(encoded1);
                });
                assertEquals("Failed to decode: OPUS_BUFFER_TOO_SMALL", e1.getMessage());

                decoder.setFrameSize(2879);
                IOException e2 = assertThrowsExactly(IOException.class, () -> {
                    decoder.decode(encoded2);
                });
                assertEquals("Failed to decode: OPUS_BUFFER_TOO_SMALL", e2.getMessage());

                decoder.setFrameSize(100_000);
                decoder.decode(encoded1);
            }
        }
    }

    @Test
    @DisplayName("Get frame size")
    void getFrameSize() throws IOException, UnknownPlatformException {
        try (OpusDecoder decoder = new OpusDecoder(48000, 1)) {
            IllegalArgumentException e1 = assertThrowsExactly(IllegalArgumentException.class, () -> {
                decoder.setFrameSize(Integer.MIN_VALUE);
            });
            assertEquals("Invalid frame size: " + Integer.MIN_VALUE, e1.getMessage());

            IllegalArgumentException e2 = assertThrowsExactly(IllegalArgumentException.class, () -> {
                decoder.setFrameSize(-1);
            });
            assertEquals("Invalid frame size: -1", e2.getMessage());

            IllegalArgumentException e3 = assertThrowsExactly(IllegalArgumentException.class, () -> {
                decoder.setFrameSize(0);
            });
            assertEquals("Invalid frame size: 0", e3.getMessage());

            decoder.setFrameSize(1);
            assertEquals(1, decoder.getFrameSize());

            decoder.setFrameSize(960);
            assertEquals(960, decoder.getFrameSize());

            decoder.setFrameSize(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, decoder.getFrameSize());
        }
    }

    @Test
    @DisplayName("Reset state")
    void resetState() throws IOException, UnknownPlatformException {
        try (OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP)) {
            byte[] encoded1 = encoder.encode(new short[120]);
            byte[] encoded2 = encoder.encode(new short[2880]);
            try (OpusDecoder decoder = new OpusDecoder(48000, 1)) {
                decoder.setFrameSize(2880);
                decoder.decode(encoded1);
                decoder.resetState();
                decoder.decode(encoded2);
                decoder.resetState();
            }
        }
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
