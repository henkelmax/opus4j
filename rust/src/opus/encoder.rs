use jni::objects::{JByteArray, JClass, JObject, JShortArray, JValue};
use jni::sys::{jbyte, jint, jlong, jshort};
use jni::JNIEnv;
use std::ffi::c_int;

extern crate libopus_sys as opus;

use crate::opus::decoder::Channels;
use crate::opus::exceptions::{
    throw_illegal_argument_exception, throw_illegal_state_exception, throw_io_exception,
    throw_runtime_exception, translate_error,
};

const DEFAULT_PAYLOAD_SIZE: u32 = 1024;

#[derive(Debug)]
struct Encoder {
    encoder: *mut opus::OpusEncoder,
    channels: Channels,
    max_payload_size: u32,
}

impl Encoder {
    pub fn new(sample_rate: u32, channels: Channels, mode: u32) -> Result<Encoder, i32> {
        let mut error = 0;
        let encoder = unsafe {
            opus::opus_encoder_create(
                sample_rate as i32,
                channels as c_int,
                mode as c_int,
                &mut error,
            )
        };
        if error as u32 != opus::OPUS_OK || encoder.is_null() {
            Err(error)
        } else {
            Ok(Encoder {
                encoder,
                channels,
                max_payload_size: DEFAULT_PAYLOAD_SIZE,
            })
        }
    }

    pub fn encode(&mut self, input: &[i16], output: &mut [u8]) -> Result<usize, i32> {
        let frame_size = (input.len() as c_int) / (self.channels as c_int);

        let result = unsafe {
            opus::opus_encode(
                self.encoder,
                input.as_ptr(),
                frame_size,
                output.as_mut_ptr(),
                output.len() as c_int,
            )
        };

        if result < 0 {
            return Err(result);
        }
        Ok(result as usize)
    }

    fn reset_state(&mut self) -> Result<(), i32> {
        return match unsafe {
            opus::opus_encoder_ctl(self.encoder, opus::OPUS_RESET_STATE as c_int)
        } {
            code if code < 0 => Err(code),
            _ => Ok(()),
        };
    }
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_createEncoder0(
    mut env: JNIEnv,
    _class: JClass,
    sample_rate: jint,
    channels: jint,
    application: JObject,
) -> jlong {
    let channels = match channels {
        1 => Channels::Mono,
        2 => Channels::Stereo,
        _ => {
            throw_illegal_argument_exception(
                &mut env,
                format!("Invalid number of channels: {}", channels),
            );
            return 0;
        }
    };

    let application = match env.get_field(application, "value", "I") {
        Ok(application) => application,
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed to get application: {}", e));
            return 0;
        }
    };

    let application = match application.i() {
        Ok(application) => application,
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed to get application: {}", e));
            return 0;
        }
    };

    let application = match application {
        1 => opus::OPUS_APPLICATION_VOIP,
        2 => opus::OPUS_APPLICATION_RESTRICTED_LOWDELAY,
        _ => opus::OPUS_APPLICATION_VOIP,
    };

    let encoder = match Encoder::new(sample_rate as u32, channels, application) {
        Ok(encoder) => encoder,
        Err(e) => {
            throw_io_exception(
                &mut env,
                format!("Failed to create encoder: {}", translate_error(e)),
            );
            return 0;
        }
    };
    return create_pointer(encoder);
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_setMaxPayloadSize0(
    mut env: JNIEnv,
    obj: JObject,
    max_payload_size: jint,
) {
    if max_payload_size <= 0 {
        throw_illegal_argument_exception(
            &mut env,
            format!("Invalid maximum payload size: {}", max_payload_size),
        );
        return;
    }

    let encoder = match get_encoder(&mut env, &obj) {
        Some(encoder) => encoder,
        None => {
            return;
        }
    };

    encoder.max_payload_size = max_payload_size as u32;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_getMaxPayloadSize0(
    mut env: JNIEnv,
    obj: JObject,
) -> jint {
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
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_encode0<'a>(
    mut env: JNIEnv<'a>,
    obj: JObject<'a>,
    input: JShortArray<'a>,
) -> JByteArray<'a> {
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

    let len = match encoder.encode(&short_array, &mut output) {
        Ok(len) => len,
        Err(e) => {
            throw_runtime_exception(
                &mut env,
                format!("Failed to encode: {}", translate_error(e)),
            );
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
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_resetState0(mut env: JNIEnv, obj: JObject) {
    let encoder = match get_encoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return;
        }
    };
    match encoder.reset_state() {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(
                &mut env,
                format!("Failed to reset state: {}", translate_error(e)),
            );
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusEncoder_destroyEncoder0(
    mut env: JNIEnv,
    obj: JObject,
) {
    let pointer = get_pointer(&mut env, &obj);

    if pointer == 0 {
        return;
    }

    let _ = unsafe { Box::from_raw(pointer as *mut Encoder) };
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
            throw_runtime_exception(
                env,
                format!("Failed to convert decoder pointer to long: {}", e),
            );
            return 0;
        }
    };
    return long;
}

fn get_encoder_from_pointer(pointer: jlong) -> &'static mut Encoder {
    let encoder = unsafe { &mut *(pointer as *mut Encoder) };
    return encoder;
}

fn get_encoder(env: &mut JNIEnv, obj: &JObject) -> Option<&'static mut Encoder> {
    let pointer = get_pointer(env, obj);
    if pointer == 0 {
        throw_illegal_state_exception(env, "Encoder is closed");
        return None;
    }
    return Some(get_encoder_from_pointer(pointer));
}

fn create_pointer(encoder: Encoder) -> jlong {
    let encoder = Box::new(encoder);
    let raw = Box::into_raw(encoder);
    return raw as jlong;
}
