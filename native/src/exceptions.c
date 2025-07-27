#include <jni.h>
#include <stdlib.h>

#include "opus_defines.h"

/**
 * Formats a string with the given printf-style format and arguments.
 *
 * @param fmt   The printf-style format string.
 * @param ...   Arguments matching the format specifiers in fmt.
 * @return      A pointer to a newly allocated, null-terminated string containing
 *              the formatted text. The caller is responsible for freeing it
 *              via free(). Returns NULL on allocation failure.
 */
char *string_format(const char *fmt, ...) {
    if (!fmt) {
        return NULL;
    }

    va_list args;
    va_start(args, fmt);

    // Determine required length
    va_list args_copy;
    va_copy(args_copy, args);
    const int needed = vsnprintf(NULL, 0, fmt, args_copy);
    va_end(args_copy);

    if (needed < 0) {
        va_end(args);
        return NULL;
    }

    // Allocate buffer (plus null terminator)
    char *buffer = malloc((size_t) needed + 1);
    if (!buffer) {
        va_end(args);
        return NULL;
    }

    // Print into buffer
    vsnprintf(buffer, (size_t) needed + 1, fmt, args);
    va_end(args);

    return buffer;
}

void throw_exception(JNIEnv *env, const char *class_name, const char *message) {
    const jclass runtime_exception = (*env)->FindClass(env, class_name);
    if (runtime_exception == NULL) {
        char *formatted = string_format("Could not find class %s", class_name);
        (*env)->FatalError(env, formatted);
        free(formatted);
        return;
    }
    (*env)->ThrowNew(env, runtime_exception, message);
}

void throw_runtime_exception(JNIEnv *env, const char *message) {
    throw_exception(env, "java/lang/RuntimeException", message);
}

void throw_illegal_state_exception(JNIEnv *env, const char *message) {
    throw_exception(env, "java/lang/IllegalStateException", message);
}

void throw_io_exception(JNIEnv *env, const char *message) {
    throw_exception(env, "java/io/IOException", message);
}

void throw_illegal_argument_exception(JNIEnv *env, const char *message) {
    throw_exception(env, "java/lang/IllegalArgumentException", message);
}

char *translate_error(const int *error) {
    switch (*error) {
        case OPUS_OK:
            return "OPUS_OK";
        case OPUS_BAD_ARG:
            return "OPUS_BAD_ARG";
        case OPUS_BUFFER_TOO_SMALL:
            return "OPUS_BUFFER_TOO_SMALL";
        case OPUS_INTERNAL_ERROR:
            return "OPUS_INTERNAL_ERROR";
        case OPUS_INVALID_PACKET:
            return "OPUS_INVALID_PACKET";
        case OPUS_UNIMPLEMENTED:
            return "OPUS_UNIMPLEMENTED";
        case OPUS_INVALID_STATE:
            return "OPUS_INVALID_STATE";
        case OPUS_ALLOC_FAIL:
            return "OPUS_ALLOC_FAIL";
        default:
            return "UNKNOWN";
    }
}

void throw_opus_io_exception(JNIEnv *env, const int *error, const char *message) {
    char *formatted = string_format("%s: %s", message, translate_error(error));
    throw_io_exception(env, formatted);
    free(formatted);
}
