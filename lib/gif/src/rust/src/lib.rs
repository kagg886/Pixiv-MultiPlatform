#![feature(lazy_get)]
use anyhow::Result;
use futures::{executor::block_on, stream, StreamExt};
use gif::{DisposalMethod, Encoder, Frame, Repeat};
use serde::Deserialize;
use std::{
    fs::File,
    ptr::slice_from_raw_parts,
    sync::{LazyLock, OnceLock},
};

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

async fn encode_animated_image(src_buffer: &[u8]) -> Result<()> {
    let GifEncodeRequest {
        metadata,
        speed,
        dstPath,
    } = serde_cbor::from_slice(src_buffer)?;
    let dst_file = File::create(dstPath)?;
    let dimen = &OnceLock::new();
    let encoder = LazyLock::new(|| {
        let (w, h) = dimen.get().unwrap();
        Encoder::new(dst_file, *w, *h, &[])
            .and_then(|mut encoder| {
                encoder.set_repeat(Repeat::Infinite)?;
                Ok(encoder)
            })
            .unwrap()
    });
    let stream = stream::iter(metadata).filter_map(|frame_info| async move {
        let file = frame_info.file;
        println!("Start decoding image {file}");
        let image = image::open(file.clone()).ok()?;
        let (width, height) = (image.width() as u16, image.height() as u16);
        dimen.get_or_init(|| (width, height));
        let mut frame = Frame::from_rgba_speed(width, height, &mut image.to_rgba8(), speed);
        println!("End decoding image {file}");
        frame.delay = (frame_info.delay / 10) as u16;
        frame.dispose = DisposalMethod::Background;
        Some(frame)
    });
    stream
        .fold(encoder, |mut encoder, frame| async move {
            LazyLock::force_mut(&mut encoder).write_frame(&frame).ok();
            encoder
        })
        .await;
    Ok(())
}

#[no_mangle]
pub extern "C" fn encode_animated_image_unsafe(ptr: *const u8, len: i32) {
    let slice = unsafe { &*slice_from_raw_parts(ptr, len as usize) };
    block_on(encode_animated_image(slice)).unwrap();
}
