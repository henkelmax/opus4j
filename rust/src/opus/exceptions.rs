use jni::JNIEnv;
extern crate libopus_sys as opus;

pub fn throw_runtime_exception<T: AsRef<str>>(env: &mut JNIEnv, message: T) {
    let _ = env.throw(("java/lang/RuntimeException", message));
}

pub fn throw_illegal_state_exception<T: AsRef<str>>(env: &mut JNIEnv, message: T) {
    let _ = env.throw(("java/lang/IllegalStateException", message));
}

pub fn throw_io_exception<T: AsRef<str>>(env: &mut JNIEnv, message: T) {
    let _ = env.throw(("java/io/IOException", message));
}

pub fn throw_illegal_argument_exception<T: AsRef<str>>(env: &mut JNIEnv, message: T) {
    let _ = env.throw(("java/lang/IllegalArgumentException", message));
}

pub fn translate_error(value: i32) -> &'static str {
    match value {
        opus::OPUS_BAD_ARG => "OPUS_BAD_ARG",
        opus::OPUS_BUFFER_TOO_SMALL => "OPUS_BUFFER_TOO_SMALL",
        opus::OPUS_INTERNAL_ERROR => "OPUS_INTERNAL_ERROR",
        opus::OPUS_INVALID_PACKET => "OPUS_INVALID_PACKET",
        opus::OPUS_UNIMPLEMENTED => "OPUS_UNIMPLEMENTED",
        opus::OPUS_INVALID_STATE => "OPUS_INVALID_STATE",
        opus::OPUS_ALLOC_FAIL => "OPUS_ALLOC_FAIL",
        _ => "UNKNOWN",
    }
}