#ifndef OPUS_DECODER_H
#define OPUS_DECODER_H

#include <jni.h>

JNIEXPORT jstring JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_getOpusVersion0(
    JNIEnv *env,
    jclass clazz
);

JNIEXPORT jlong JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_createDecoder0(
    JNIEnv *env,
    jclass clazz,
    jint sample_rate,
    jint channels
);

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_setFrameSize0(
    JNIEnv *env,
    jobject obj,
    jint frame_size
);

JNIEXPORT jint JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_getFrameSize0(
    JNIEnv *env,
    jobject obj
);

JNIEXPORT jshortArray JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_decode0(
    JNIEnv *env,
    jobject obj,
    jbyteArray input,
    jboolean fec
);

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_resetState0(
    JNIEnv *env,
    jobject obj
);

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusDecoder_destroyDecoder0(
    JNIEnv *env,
    jobject obj
);

#endif