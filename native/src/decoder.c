#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>

#include "exceptions.h"
#include "opus.h"

#define DEFAULT_FRAME_SIZE 960

typedef struct Decoder {
    OpusDecoder *decoder;
    int frame_size;
    int channels;
} Decoder;

/**
 * @param sample_rate the sample rate
 * @param channels the number of channels
 * @param error the error if the decoder could not be created
 * @return the decoder or NULL if the decoder could not be created
 */
Decoder *create_decoder(const opus_int32 sample_rate, const int channels, int *error) {
    Decoder *decoder = malloc(sizeof(Decoder));
    int err = 0;
    decoder->decoder = opus_decoder_create(sample_rate, channels, &err);
    *error = err;
    if (err != OPUS_OK) {
        free(decoder);
        return NULL;
    }
    decoder->frame_size = DEFAULT_FRAME_SIZE;
    decoder->channels = channels;
    return decoder;
}

void destroy_decoder(Decoder *decoder) {
    opus_decoder_destroy(decoder->decoder);
    free(decoder);
}

/**
 * Gets the decoder from the decoder java object.
 *
 * @param env the JNI environment
 * @param decoder_pointer the pointer to the decoder
 * @return the decoder or NULL - If the decoder could not be retrieved, this will throw a runtime exception in Java
 */
Decoder *get_decoder(JNIEnv *env, jlong decoder_pointer) {
    if (decoder_pointer == 0) {
        throw_runtime_exception(env, "Decoder is closed");
        return NULL;
    }
    return (Decoder *) (uintptr_t) decoder_pointer;
}

JNIEXPORT jstring JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_getOpusVersion0(
    JNIEnv *env,
    jclass clazz
) {
    return (*env)->NewStringUTF(env, opus_get_version_string());
}

JNIEXPORT jlong JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_createDecoder0(
    JNIEnv *env,
    jclass clazz,
    jint sample_rate,
    jint channels
) {
    if (channels != 1 && channels != 2) {
        char *message = string_format("Invalid number of channels: %d", channels);
        throw_illegal_argument_exception(env, message);
        free(message);
        return 0;
    }

    int err = 0;
    Decoder *decoder = create_decoder(sample_rate, channels, &err);
    if (err != OPUS_OK) {
        throw_opus_io_exception(env, err, "Failed to create decoder");
        if (decoder != NULL) {
            destroy_decoder(decoder);
        }
        return 0;
    }

    return (jlong) (uintptr_t) decoder;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_setFrameSize0(
    JNIEnv *env,
    jobject obj,
    jlong decoder_pointer,
    jint frame_size
) {
    if (frame_size <= 0) {
        char *message = string_format("Invalid frame size: %d", frame_size);
        throw_illegal_argument_exception(env, message);
        free(message);
        return;
    }
    Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return;
    }
    decoder->frame_size = frame_size;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_getFrameSize0(
    JNIEnv *env,
    jobject obj,
    jlong decoder_pointer
) {
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return 0;
    }
    return decoder->frame_size;
}

JNIEXPORT jshortArray JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_decode0(
    JNIEnv *env,
    jobject obj,
    jlong decoder_pointer,
    jbyteArray input,
    jboolean fec
) {
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return NULL;
    }

    bool use_fec = fec;
    int input_length;
    jbyte *opus_input;

    if (input == NULL) {
        use_fec = true;
        input_length = 0;
        opus_input = NULL;
    } else {
        input_length = (*env)->GetArrayLength(env, input);
        opus_input = (*env)->GetByteArrayElements(env, input, false);
    }

    const int output_length = decoder->frame_size * decoder->channels;

    opus_int16 *opus_output = calloc(output_length, sizeof(opus_int16));

    const int result = opus_decode(decoder->decoder, (unsigned char *) opus_input, input_length, opus_output,
                                   decoder->frame_size, use_fec);

    if (input != NULL) {
        (*env)->ReleaseByteArrayElements(env, input, opus_input, JNI_ABORT);
    }

    if (result < 0) {
        throw_opus_io_exception(env, result, "Failed to decode");
        free(opus_output);
        return NULL;
    }

    if (result > output_length) {
        char *message = string_format("Invalid output length: %d>%d", result, output_length);
        throw_illegal_state_exception(env, message);
        free(message);
        free(opus_output);
        return NULL;
    }

    const int total_samples = result * decoder->channels;
    const jshortArray java_output = (*env)->NewShortArray(env, total_samples);
    (*env)->SetShortArrayRegion(env, java_output, 0, total_samples, opus_output);
    free(opus_output);
    return java_output;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_resetState0(
    JNIEnv *env,
    jobject obj,
    jlong decoder_pointer
) {
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return;
    }
    const int err = opus_decoder_ctl(decoder->decoder, OPUS_RESET_STATE);
    if (err != OPUS_OK) {
        throw_opus_io_exception(env, err, "Failed to reset state");
    }
}

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_destroyDecoder0(
    JNIEnv *env,
    jobject obj,
    jlong decoder_pointer
) {
    if (decoder_pointer == 0) {
        return;
    }
    Decoder *decoder = (Decoder *) (uintptr_t) decoder_pointer;
    destroy_decoder(decoder);
}
