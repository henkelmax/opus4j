use jni::objects::{JByteArray, JClass, JObject, JShortArray, JValue};
use jni::sys::{jboolean, jint, jlong};
use jni::JNIEnv;
use std::ffi::c_int;
use std::ptr;

extern crate libopus_sys as opus;

use crate::opus::exceptions::{
    throw_illegal_argument_exception, throw_illegal_state_exception, throw_io_exception,
    throw_runtime_exception, translate_error,
};

const DEFAULT_FRAME_SIZE: u32 = 960;

#[derive(Debug)]
struct Decoder {
    decoder: *mut opus::OpusDecoder,
    frame_size: u32,
    channels: Channels,
}

impl Decoder {
    pub fn new(sample_rate: u32, frame_size: u32, channels: Channels) -> Result<Decoder, i32> {
        let mut error = 0;
        let decoder =
            unsafe { opus::opus_decoder_create(sample_rate as i32, channels as c_int, &mut error) };
        if error as u32 != opus::OPUS_OK || decoder.is_null() {
            Err(error as i32)
        } else {
            Ok(Decoder {
                decoder,
                frame_size,
                channels,
            })
        }
    }

    pub fn decode(&mut self, input: &[u8], output: &mut [i16], fec: bool) -> Result<usize, i32> {
        let data_ptr;
        if input.is_empty() {
            data_ptr = ptr::null()
        } else {
            data_ptr = input.as_ptr()
        }

        let data_len = input.len() as c_int;

        let frame_size = (output.len() / self.channels as usize) as c_int;

        let decode_fec = if fec { 1 as c_int } else { 0 as c_int };

        let result = unsafe {
            opus::opus_decode(
                self.decoder,
                data_ptr,
                data_len,
                output.as_mut_ptr(),
                frame_size,
                decode_fec,
            )
        };

        if result < 0 {
            return Err(result);
        }

        Ok(result as usize)
    }

    fn reset_state(&mut self) -> Result<(), i32> {
        return match unsafe {
            opus::opus_decoder_ctl(self.decoder, opus::OPUS_RESET_STATE as c_int)
        } {
            code if code < 0 => Err(code),
            _ => Ok(()),
        };
    }
}

#[derive(Debug, Clone, Copy, Eq, PartialEq, Hash)]
pub enum Channels {
    Mono = 1,
    Stereo = 2,
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_createDecoder0(
    mut env: JNIEnv,
    _class: JClass,
    sample_rate: jint,
    channels: jint,
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
    let decoder = match Decoder::new(sample_rate as u32, DEFAULT_FRAME_SIZE, channels) {
        Ok(decoder) => decoder,
        Err(e) => {
            throw_io_exception(
                &mut env,
                format!("Failed to create decoder: {}", translate_error(e)),
            );
            return 0;
        }
    };
    return create_pointer(decoder);
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_setFrameSize0(
    mut env: JNIEnv,
    obj: JObject,
    frame_size: jint,
) {
    if frame_size <= 0 {
        throw_illegal_argument_exception(&mut env, format!("Invalid frame size: {}", frame_size));
        return;
    }

    let decoder = match get_decoder(&mut env, &obj) {
        Some(encoder) => encoder,
        None => {
            return;
        }
    };

    decoder.frame_size = frame_size as u32;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_getFrameSize0(
    mut env: JNIEnv,
    obj: JObject,
) -> jint {
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
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_decode0<'a>(
    mut env: JNIEnv<'a>,
    obj: JObject<'a>,
    input: JByteArray<'a>,
    fec: jboolean,
) -> JShortArray<'a> {
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

    let len = match decoder.decode(input_vec.as_slice(), &mut output, fec != 0) {
        Ok(len) => len,
        Err(e) => {
            throw_runtime_exception(
                &mut env,
                format!("Failed to decode: {}", translate_error(e)),
            );
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
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_resetState0(mut env: JNIEnv, obj: JObject) {
    let decoder = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return;
        }
    };
    match decoder.reset_state() {
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
pub extern "C" fn Java_de_maxhenkel_opus4j_OpusDecoder_destroyDecoder0(
    mut env: JNIEnv,
    obj: JObject,
) {
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
            throw_runtime_exception(
                env,
                format!("Failed to convert decoder pointer to long: {}", e),
            );
            return 0;
        }
    };
    return long;
}

fn get_decoder_from_pointer(pointer: jlong) -> &'static mut Decoder {
    let decoder = unsafe { &mut *(pointer as *mut Decoder) };
    return decoder;
}

fn get_decoder(env: &mut JNIEnv, obj: &JObject) -> Option<&'static mut Decoder> {
    let pointer = get_pointer(env, obj);
    if pointer == 0 {
        throw_illegal_state_exception(env, "Decoder is closed");
        return None;
    }
    return Some(get_decoder_from_pointer(pointer));
}

fn create_pointer(decoder: Decoder) -> jlong {
    let decoder = Box::new(decoder);
    let raw = Box::into_raw(decoder);
    return raw as jlong;
}
