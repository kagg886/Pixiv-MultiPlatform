use std::{fs::File, ptr::slice_from_raw_parts};
use anyhow::Result;
use gif::{DisposalMethod, Encoder, Frame, Repeat};
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
    width: u16,
    height: u16,
}

fn encode_animated_image(src_buffer: &[u8]) -> Result<()> {
    let GifEncodeRequest { metadata, speed, dstPath, width, height } = serde_cbor::from_slice(src_buffer)?;
    let dst_file = File::create(dstPath)?;
    let mut encoder = Encoder::new(dst_file, width, height, &[])?;
    encoder.set_repeat(Repeat::Infinite)?;
    for frame_info in metadata {
        let image = image::open(frame_info.file)?;
        let mut frame = Frame::from_rgba_speed(width, height, &mut image.to_rgba8(), speed);
        frame.delay = (frame_info.delay / 10) as u16;
        frame.dispose = DisposalMethod::Background;
        encoder.write_frame(&frame)?;
    }
    Ok(())
}

#[no_mangle]
pub extern "C" fn encode_animated_image_unsafe(ptr: *const u8, len: i32) {
    let slice = unsafe { &*slice_from_raw_parts(ptr, len as usize) };
    encode_animated_image(slice).unwrap()
}
