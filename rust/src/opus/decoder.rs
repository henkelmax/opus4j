use jni::{JNIEnv};
use jni::objects::{JByteArray, JClass, JObject, JShortArray, JValue};
use jni::sys::{jboolean, jint, jlong};
use opus::{Channels, Decoder};
use crate::opus::exceptions::{throw_illegal_argument_exception, throw_illegal_state_exception, throw_io_exception, throw_runtime_exception};

const DEFAULT_FRAME_SIZE: u32 = 960;

struct DecoderWrapper {
    decoder: Decoder,
    frame_size: u32,
    channels: Channels,
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_createDecoder(mut env: JNIEnv, _class: JClass, sample_rate: jint, channels: jint) -> jlong {
    let channels = match channels {
        1 => Channels::Mono,
        2 => Channels::Stereo,
        _ => {
            throw_illegal_argument_exception(&mut env, format!("Invalid number of channels: {}", channels));
            return 0;
        }
    };
    let decoder = match Decoder::new(sample_rate as u32, channels) {
        Ok(decoder) => decoder,
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed to create decoder: {}", e.description()));
            return 0;
        }
    };
    return create_pointer(DecoderWrapper {
        decoder,
        frame_size: DEFAULT_FRAME_SIZE,
        channels,
    });
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_setFrameSize(mut env: JNIEnv, obj: JObject, frame_size: jint) {
    if frame_size <= 0 {
        throw_illegal_argument_exception(&mut env, format!("Invalid frame size: {}", frame_size));
        return;
    }

    let mut decoder = match get_decoder(&mut env, &obj) {
        Some(encoder) => encoder,
        None => {
            return;
        }
    };

    decoder.frame_size = frame_size as u32;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_getFrameSize(mut env: JNIEnv, obj: JObject) -> jint {
    let decoder = match get_decoder(&mut env, &obj) {
        Some(encoder) => encoder,
        None => {
            throw_runtime_exception(&mut env, "Failed to read frame size");
            return 0;
        }
    };

    return decoder.frame_size as jint;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_decode<'a>(mut env: JNIEnv<'a>, obj: JObject<'a>, input: JByteArray<'a>, fec: jboolean) -> JShortArray<'a> {
    let decoder = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return JShortArray::from(JObject::null());
        }
    };

    let mut fec: u8 = match fec.try_into() {
        Ok(fec) => fec,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to convert boolean: {}", e));
            return JShortArray::from(JObject::null());
        }
    };

    let input_vec: Vec<u8>;

    if input.as_raw().is_null() {
        fec = 1;
        input_vec = vec![0u8; 0];
    } else {
        input_vec = match env.convert_byte_array(input) {
            Ok(input) => input,
            Err(e) => {
                throw_runtime_exception(&mut env, format!("Failed to convert byte array: {}", e));
                return JShortArray::from(JObject::null());
            }
        };
    }

    let mut output = vec![0i16; decoder.frame_size as usize * decoder.channels as usize];

    let len = match decoder.decoder.decode(input_vec.as_slice(), &mut output, fec != 0) {
        Ok(len) => len,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to decode: {}", e.description()));
            return JShortArray::from(JObject::null());
        }
    };

    output.truncate(len);
    let output = output.as_slice();

    let short_array = match env.new_short_array(output.len() as i32) {
        Ok(jshort) => jshort,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to create short array: {}", e));
            return JShortArray::from(JObject::null());
        }
    };
    match env.set_short_array_region(&short_array, 0, output) {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed populate short array: {}", e));
            return JShortArray::from(JObject::null());
        }
    }
    return short_array;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_resetState(mut env: JNIEnv, obj: JObject) {
    let decoder = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return;
        }
    };
    match decoder.decoder.reset_state() {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to reset state: {}", e.description()));
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_destroyDecoder(mut env: JNIEnv, obj: JObject) {
    let pointer = get_pointer(&mut env, &obj);

    if pointer == 0 {
        return;
    }

    let _ = unsafe { Box::from_raw(pointer as *mut Decoder) };
    let _ = env.set_field(obj, "decoder", "J", JValue::from(jlong::from(0)));
}

fn get_pointer(env: &mut JNIEnv, obj: &JObject) -> jlong {
    let pointer = match env.get_field(obj, "decoder", "J") {
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

fn get_decoder_from_pointer(pointer: jlong) -> &'static mut DecoderWrapper {
    let decoder = unsafe { &mut *(pointer as *mut DecoderWrapper) };
    return decoder;
}

fn get_decoder(env: &mut JNIEnv, obj: &JObject) -> Option<&'static mut DecoderWrapper> {
    let pointer = get_pointer(env, obj);
    if pointer == 0 {
        throw_illegal_state_exception(env, "Decoder is closed");
        return None;
    }
    return Some(get_decoder_from_pointer(pointer));
}

fn create_pointer(decoder: DecoderWrapper) -> jlong {
    let decoder = Box::new(decoder);
    let raw = Box::into_raw(decoder);
    return raw as jlong;
}