use jni::{JNIEnv};
use jni::objects::{JByteArray, JClass, JObject, JShortArray, JValue};
use jni::sys::{jbyte, jint, jlong, jshort};
use opus::{Application, Channels, Encoder};

const DEFAULT_PAYLOAD_SIZE: u32 = 1024;

struct EncoderWrapper {
    encoder: Encoder,
    max_payload_size: u32,
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_createEncoder(mut env: JNIEnv, _class: JClass, sample_rate: jint, channels: jint, application: JObject) -> jlong {
    let channels = match channels {
        1 => Channels::Mono,
        2 => Channels::Stereo,
        _ => {
            let _ = env.throw(("java/lang/IllegalArgumentException", format!("Invalid number of channels: {}", channels)));
            return 0;
        }
    };

    let application = match env.get_field(application, "value", "I") {
        Ok(application) => application,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to get application: {}", e));
            return 0;
        }
    };

    let application = match application.i() {
        Ok(application) => application,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to convert application to int: {}", e));
            return 0;
        }
    };

    let application = match application {
        1 => Application::Audio,
        2 => Application::LowDelay,
        _ => Application::Voip,
    };

    let encoder = match Encoder::new(sample_rate as u32, channels, application) {
        Ok(encoder) => encoder,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to create encoder: {}", e.description()));
            return 0;
        }
    };
    return create_pointer(EncoderWrapper {
        encoder,
        max_payload_size: DEFAULT_PAYLOAD_SIZE,
    });
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_setMaxPayloadSize(mut env: JNIEnv, obj: JObject, max_payload_size: jint) {
    if max_payload_size <= 0 {
        let _ = env.throw(("java/lang/IllegalArgumentException", format!("Invalid maximum payload size: {}", max_payload_size)));
        return;
    }

    let mut encoder = match get_encoder(&mut env, &obj) {
        Some(encoder) => encoder,
        None => {
            return;
        }
    };

    encoder.max_payload_size = max_payload_size as u32;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_getMaxPayloadSize(mut env: JNIEnv, obj: JObject) -> jint {
    let encoder = match get_encoder(&mut env, &obj) {
        Some(encoder) => encoder,
        None => {
            throw_runtime_exception(&mut env, "Failed to read max payload size".to_owned());
            return 0;
        }
    };

    return encoder.max_payload_size as jint;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_encode<'a>(mut env: JNIEnv<'a>, obj: JObject<'a>, input: JShortArray<'a>) -> JByteArray<'a> {
    let encoder = match get_encoder(&mut env, &obj) {
        Some(encoder) => encoder,
        None => {
            return JByteArray::from(JObject::null());
        }
    };

    let input_length = match env.get_array_length(&input) {
        Ok(input_length) => input_length as usize,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to get input length: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    let mut short_array = vec![0i16 as jshort; input_length];

    match env.get_short_array_region(input, 0, &mut short_array) {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to convert short array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    let mut output = vec![0u8; encoder.max_payload_size as usize];

    let len = match encoder.encoder.encode(&short_array, &mut output) {
        Ok(len) => len,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to encode: {}", e.description()));
            return JByteArray::from(JObject::null());
        }
    };

    output.truncate(len);

    let mut output = unsafe { &*(output.as_mut_slice() as *mut [u8] as *mut [jbyte]) };

    let byte_array = match env.new_byte_array(output.len() as i32) {
        Ok(output) => output,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to create byte array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    match env.set_byte_array_region(&byte_array, 0, &mut output) {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed populate byte array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    return byte_array;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_resetState(mut env: JNIEnv, obj: JObject) {
    let encoder = match get_encoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return;
        }
    };
    match encoder.encoder.reset_state() {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to reset state: {}", e.description()));
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_destroyEncoder(mut env: JNIEnv, obj: JObject) {
    let pointer = get_pointer(&mut env, &obj);

    if pointer == 0 {
        return;
    }

    let _ = unsafe { Box::from_raw(pointer as *mut EncoderWrapper) };
    let _ = env.set_field(obj, "encoder", "J", JValue::from(jlong::from(0)));
}

fn get_pointer(env: &mut JNIEnv, obj: &JObject) -> jlong {
    let pointer = match env.get_field(obj, "encoder", "J") {
        Ok(pointer) => pointer,
        Err(e) => {
            throw_runtime_exception(env, format!("Failed to get decoder pointer: {}", e));
            return 0;
        }
    };
    let long = match pointer.j() {
        Ok(long) => long,
        Err(e) => {
            throw_runtime_exception(env, format!("Failed to convert decoder pointer to long: {}", e));
            return 0;
        }
    };
    return long;
}

fn get_encoder_from_pointer(pointer: jlong) -> &'static mut EncoderWrapper {
    let encoder = unsafe { &mut *(pointer as *mut EncoderWrapper) };
    return encoder;
}

fn get_encoder(env: &mut JNIEnv, obj: &JObject) -> Option<&'static mut EncoderWrapper> {
    let pointer = get_pointer(env, obj);
    if pointer == 0 {
        let _ = env.throw(("java/lang/IllegalStateException", "Encoder is closed"));
        return None;
    }
    return Some(get_encoder_from_pointer(pointer));
}

fn create_pointer(encoder: EncoderWrapper) -> jlong {
    let decoder = Box::new(encoder);
    let raw = Box::into_raw(decoder);
    return raw as jlong;
}

fn throw_runtime_exception(env: &mut JNIEnv, message: String) {
    let _ = env.throw(("java/lang/RuntimeException", message));
}