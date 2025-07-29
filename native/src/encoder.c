#include <jni.h>
#include <inttypes.h>
#include <stdlib.h>
#include <stdbool.h>

#include "opus.h"
#include "exceptions.h"

#define DEFAULT_MAX_PAYLOAD_SIZE 1024
#define MAX_MAX_PAYLOAD_SIZE 4096

typedef struct Encoder {
    OpusEncoder *encoder;
    uint32_t channels;
    int max_payload_size;
} Encoder;

/**
 *
 * @param sample_rate the sample rate
 * @param channels the number of channels
 * @param application the application
 * @param error the error if the encoder could not be created
 * @return the encoder or NULL if the encoder could not be created
 */
Encoder *create_encoder(const opus_int32 sample_rate, const int channels, const int application, int *error) {
    Encoder *encoder = malloc(sizeof(Encoder));
    int err = 0;
    encoder->encoder = opus_encoder_create(sample_rate, channels, application, &err);
    *error = err;
    if (err != OPUS_OK) {
        free(encoder);
        return NULL;
    }
    encoder->channels = channels;
    encoder->max_payload_size = DEFAULT_MAX_PAYLOAD_SIZE;
    return encoder;
}

void destroy_encoder(Encoder *encoder) {
    opus_encoder_destroy(encoder->encoder);
    free(encoder);
}

/**
 * Gets the encoder from the encoder java object.
 *
 * @param env the JNI environment
 * @param encoder_pointer the pointer to the encoder
 * @return the encoder or NULL - If the encoder could not be retrieved, this will throw a runtime exception in Java
 */
Encoder *get_encoder(JNIEnv *env, jlong encoder_pointer) {
    const jlong pointer = encoder_pointer;
    if (pointer == 0) {
        throw_runtime_exception(env, "Encoder is closed");
        return NULL;
    }
    return (Encoder *) (uintptr_t) pointer;
}

JNIEXPORT jstring JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_getOpusVersion0(
    JNIEnv *env,
    jclass clazz
) {
    return (*env)->NewStringUTF(env, opus_get_version_string());
}

JNIEXPORT jlong JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_createEncoder0(
    JNIEnv *env,
    jclass clazz,
    jint sample_rate,
    jint channels,
    jobject application
) {
    if (channels != 1 && channels != 2) {
        char *message = string_format("Invalid number of channels: %d", channels);
        throw_illegal_argument_exception(env, message);
        free(message);
        return 0;
    }
    const jint application_int = (*env)->GetIntField(env, application,
                                                     (*env)->GetFieldID(env, (*env)->GetObjectClass(env, application),
                                                                        "value", "I"));

    int opus_application;
    switch (application_int) {
        case 1:
            opus_application = OPUS_APPLICATION_AUDIO;
            break;
        case 2:
            opus_application = OPUS_APPLICATION_RESTRICTED_LOWDELAY;
            break;
        default:
            opus_application = OPUS_APPLICATION_VOIP;
            break;
    }

    int err = 0;
    Encoder *encoder = create_encoder(sample_rate, channels, opus_application, &err);
    if (err != OPUS_OK) {
        throw_opus_io_exception(env, &err, "Failed to create encoder");
        if (encoder != NULL) {
            destroy_encoder(encoder);
        }
        return 0;
    }

    return (jlong) (uintptr_t) encoder;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_setMaxPayloadSize0(
    JNIEnv *env,
    jobject obj,
    jlong encoder_pointer,
    jint max_payload_size
) {
    if (max_payload_size <= 0) {
        char *message = string_format("Invalid maximum payload size: %d", max_payload_size);
        throw_illegal_argument_exception(env, message);
        free(message);
        return;
    }
    if (max_payload_size > MAX_MAX_PAYLOAD_SIZE) {
        char *message = string_format("Maximum payload size too large: %d", max_payload_size);
        throw_illegal_argument_exception(env, message);
        free(message);
        return;
    }
    Encoder *encoder = get_encoder(env, encoder_pointer);
    if (encoder == NULL) {
        return;
    }
    encoder->max_payload_size = max_payload_size;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_getMaxPayloadSize0(
    JNIEnv *env,
    jobject obj,
    jlong encoder_pointer
) {
    const Encoder *encoder = get_encoder(env, encoder_pointer);
    if (encoder == NULL) {
        return 0;
    }
    return encoder->max_payload_size;
}

JNIEXPORT jbyteArray JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_encode0(
    JNIEnv *env,
    jobject obj,
    jlong encoder_pointer,
    jshortArray input
) {
    const Encoder *encoder = get_encoder(env, encoder_pointer);
    if (encoder == NULL) {
        return NULL;
    }
    const int input_length = (*env)->GetArrayLength(env, input);
    const int max_payload_size = encoder->max_payload_size;

    const opus_int16 *opus_input = (*env)->GetShortArrayElements(env, input, false);

    unsigned char *output = malloc(max_payload_size);

    const int result = opus_encode(encoder->encoder, opus_input, input_length, output, max_payload_size);
    (*env)->ReleaseShortArrayElements(env, input, (jshort *) opus_input, JNI_ABORT);
    if (result < 0) {
        free(output);
        throw_opus_io_exception(env, &result, "Failed to encode");
        return NULL;
    }
    const jbyteArray java_output = (*env)->NewByteArray(env, result);
    (*env)->SetByteArrayRegion(env, java_output, 0, result, (jbyte *) output);
    free(output);
    return java_output;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_resetState0(
    JNIEnv *env,
    jobject obj,
    jlong encoder_pointer
) {
    const Encoder *encoder = get_encoder(env, encoder_pointer);
    if (encoder == NULL) {
        return;
    }
    const int err = opus_encoder_ctl(encoder->encoder, OPUS_RESET_STATE);
    if (err != OPUS_OK) {
        throw_opus_io_exception(env, &err, "Failed to reset state");
    }
}

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_destroyEncoder0(
    JNIEnv *env,
    jobject obj,
    jlong encoder_pointer
) {
    if (encoder_pointer == 0) {
        return;
    }
    Encoder *encoder = (Encoder *) (uintptr_t) encoder_pointer;
    destroy_encoder(encoder);
}
