#ifndef OPUS_ENCODER_H
#define OPUS_ENCODER_H

#include <jni.h>

JNIEXPORT jstring JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_getOpusVersion0(
    JNIEnv *env,
    jclass clazz
);

JNIEXPORT jlong JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_createEncoder0(
    JNIEnv *env,
    jclass clazz,
    jint sample_rate,
    jint channels,
    jobject application
);

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_setMaxPayloadSize0(
    JNIEnv *env,
    jobject obj,
    jint max_payload_size
);

JNIEXPORT jint JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_getMaxPayloadSize0(
    JNIEnv *env,
    jobject obj
);

JNIEXPORT jbyteArray JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_encode0(
    JNIEnv *env,
    jobject obj,
    jshortArray input
);

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_resetState0(
    JNIEnv *env,
    jobject obj
);

JNIEXPORT void JNICALL Java_de_maxhenkel_opus4j_OpusEncoder_destroyEncoder0(
    JNIEnv *env,
    jobject obj
);

#endif