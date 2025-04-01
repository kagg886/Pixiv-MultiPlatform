use std::{fs::File, ptr::slice_from_raw_parts, time::Duration};
use anyhow::Result;
use image::{codecs::gif::{GifEncoder, Repeat}, Delay, Frame};
use serde::Deserialize;

#[derive(Deserialize)]
#[allow(non_snake_case)]
struct UgoiraFrame {
    file: String,
    delay: u64,
}

#[derive(Deserialize)]
#[allow(non_snake_case)]
struct GifEncodeRequest {
    metadata: Vec<UgoiraFrame>,
    speed: i32,
    dstPath: String,
}

fn encode_animated_image(src_buffer: &[u8]) -> Result<()> {
    let request: GifEncodeRequest = serde_cbor::from_slice(src_buffer)?;
    let dst_file = File::create(request.dstPath)?;
    let mut encoder = GifEncoder::new_with_speed(dst_file, request.speed);
    encoder.set_repeat(Repeat::Infinite)?;
    for frame_info in request.metadata {
        let image = image::open(frame_info.file)?;
        let delay = Delay::from_saturating_duration(Duration::from_millis(frame_info.delay));
        let frame = Frame::from_parts(image.into_rgba8(), 0, 0, delay);
        encoder.encode_frame(frame)?
    }
    Ok(())
}

#[no_mangle]
pub extern "C" fn encode_animated_image_unsafe(ptr: *const u8, len: i32) {
    let slice = unsafe { &*slice_from_raw_parts(ptr, len as usize) };
    encode_animated_image(slice).unwrap()
}
