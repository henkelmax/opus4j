#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>

#include "exceptions.h"
#include "opus.h"

#define DEFAULT_FRAME_SIZE 960

#define MIN(a, b) ( ( (a) < (b) ) ? (a) : (b) )

typedef struct Decoder {
    OpusDecoder *decoder;
    OpusDREDDecoder *dred_decoder;
    OpusDRED *dred;
    opus_int32 sample_rate;
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
    if (err < 0) {
        free(decoder);
        return NULL;
    }
    decoder->dred_decoder = opus_dred_decoder_create(&err);
    *error = err;
    if (err < 0) {
        free(decoder->decoder);
        free(decoder);
        return NULL;
    }
    decoder->dred = opus_dred_alloc(&err);
    *error = err;
    if (err < 0) {
        free(decoder->decoder);
        free(decoder->dred_decoder);
        free(decoder);
        return NULL;
    }
    decoder->sample_rate = sample_rate;
    decoder->frame_size = DEFAULT_FRAME_SIZE;
    decoder->channels = channels;
    return decoder;
}

void destroy_decoder(Decoder *decoder) {
    opus_decoder_destroy(decoder->decoder);
    opus_dred_decoder_destroy(decoder->dred_decoder);
    opus_dred_free(decoder->dred);
    free(decoder);
}

/**
 * Gets the decoder from the decoder java object.
 *
 * @param env the JNI environment
 * @param decoder_pointer the pointer to the decoder
 * @return the decoder or NULL - If the decoder could not be retrieved, this will throw a runtime exception in Java
 */
Decoder *get_decoder(JNIEnv *env, const jlong decoder_pointer) {
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
    if (err < 0) {
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
    const jlong decoder_pointer,
    const jint frame_size
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
    const jlong decoder_pointer
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
    const jlong decoder_pointer,
    const jbyteArray input,
    const jboolean fec
) {
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return NULL;
    }

    bool use_fec = fec;
    jsize input_length;
    unsigned char *opus_input;

    if (input == NULL) {
        use_fec = true;
        input_length = 0;
        opus_input = NULL;
    } else {
        input_length = (*env)->GetArrayLength(env, input);
        opus_input = (unsigned char *) (*env)->GetByteArrayElements(env, input, false);
    }

    int dred_end; // will be set to the number of samples covered by DRED
    // 4) Parse the DRED info out of the packet
    opus_dred_parse(
        decoder->dred_decoder, // DRED‐decoder state
        decoder->dred, // DRED feature container
        opus_input, input_length, // incoming Opus packet
        /* max_dred_samples */ decoder->frame_size * 10, // e.g. up to 200 ms @20 ms frames
        decoder->sample_rate, // sampling rate
        &dred_end, // how many past samples we now have features for
        1 // defer_processing = 1 -> we need to call opus_dred_process()
    );

    const int output_length = decoder->frame_size * decoder->channels;

    opus_int16 *opus_output = calloc(output_length, sizeof(opus_int16));

    const int result = opus_decode(decoder->decoder, opus_input, input_length, opus_output,
                                   decoder->frame_size, use_fec);

    if (input != NULL) {
        (*env)->ReleaseByteArrayElements(env, input, (jbyte *) opus_input, JNI_ABORT);
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

jobjectArray create_short_short_array(JNIEnv *env, const int length, const int inner_length) {
    const jclass shortArrayCls = (*env)->FindClass(env, "[S");
    if (shortArrayCls == NULL) {
        throw_illegal_state_exception(env, "Failed to find short array class");
        return NULL;
    }

    const jobjectArray short_short_array = (*env)->NewObjectArray(env, length, shortArrayCls, NULL);
    for (int i = 0; i < length; i++) {
        (*env)->SetObjectArrayElement(env, short_short_array, i, (*env)->NewShortArray(env, inner_length));
    }
    return short_short_array;
}

void fill_short_short_array(JNIEnv *env, const jobjectArray short_short_array, const int index, const opus_int16 *data,
                            const int length) {
    const jshortArray short_array = (*env)->GetObjectArrayElement(env, short_short_array, index);
    const jsize array_length = (*env)->GetArrayLength(env, short_array);
    if (array_length != length) {
        (*env)->DeleteLocalRef(env, short_array);
        //TODO Check what we should do here
        return;
    }
    (*env)->SetShortArrayRegion(env, short_array, 0, length, data);
    (*env)->DeleteLocalRef(env, short_array);
}

JNIEXPORT jobjectArray JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_recover0(
    JNIEnv *env,
    jobject obj,
    const jlong decoder_pointer,
    const jbyteArray input,
    const jint max_frames
) {
    if (max_frames <= 0) {
        throw_illegal_argument_exception(env, "Max frames must be greater than 0");
        return NULL;
    }
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return NULL;
    }
    if (input == NULL) {
        throw_illegal_argument_exception(env, "Can't recover without input");
        return NULL;
    }

    const jsize input_length = (*env)->GetArrayLength(env, input);
    unsigned char *opus_input = (unsigned char *) (*env)->GetByteArrayElements(env, input, false);

    int dred_end;
    opus_dred_parse(
        decoder->dred_decoder,
        decoder->dred,
        opus_input,
        input_length,
        decoder->frame_size * 10,
        decoder->sample_rate,
        &dred_end,
        0
    );

    printf("dred_end: %d\n", dred_end);

    const int max_recoverable_frames = dred_end / decoder->frame_size;

    const int output_length = decoder->frame_size * decoder->channels;

    opus_int16 *opus_output = calloc(output_length, sizeof(opus_int16));

    int result = opus_decode(decoder->decoder, opus_input, input_length, opus_output,
                             decoder->frame_size, false);

    (*env)->ReleaseByteArrayElements(env, input, (jbyte *) opus_input, JNI_ABORT);

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

    const int frames_to_recover = MIN(max_frames, max_recoverable_frames + 1);

    const jobjectArray recovered = create_short_short_array(env, frames_to_recover, output_length);

    fill_short_short_array(env, recovered, frames_to_recover - 1, opus_output, result * decoder->channels);

    for (int i = 0; i < max_recoverable_frames; i++) {
        result = opus_decoder_dred_decode(
            decoder->decoder,
            decoder->dred,
            (i + 1) * decoder->frame_size,
            opus_output,
            decoder->frame_size
        );
        if (result < 0) {
            throw_opus_io_exception(env, result, "Failed to recover");
            free(opus_output);
            return NULL;
        }
        if (result != output_length) {
            char *message = string_format("Invalid output length: %d!=%d", result, output_length);
            throw_illegal_state_exception(env, message);
            free(message);
            free(opus_output);
            return NULL;
        }
        fill_short_short_array(env, recovered, i, opus_output, result * decoder->channels);
    }

    free(opus_output);
    return recovered;
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
    if (err < 0) {
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
