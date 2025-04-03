#![feature(lazy_get)]
mod jvm;
use anyhow::Result;
use gif::{DisposalMethod, Encoder, Frame, Repeat};
use serde::Deserialize;
use std::{fs::File, ptr::slice_from_raw_parts, sync::{Arc, LazyLock, OnceLock}};
use tokio::runtime::Runtime;

#[derive(Deserialize, Clone)]
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

async fn encode_animated_image(src_buffer: &[u8], rt: &Runtime) -> Result<()> {
    let GifEncodeRequest { metadata, speed, dstPath } = serde_cbor::from_slice(src_buffer)?;
    let dst_file = File::create(dstPath)?;
    let dimen = Arc::new(OnceLock::new());
    let mut encoder = LazyLock::new(|| {
        let (w, h) = dimen.get().unwrap();
        Encoder::new(dst_file, *w, *h, &[])
            .and_then(|mut encoder| {
                encoder.set_repeat(Repeat::Infinite)?;
                Ok(encoder)
            })
            .unwrap()
    });
    let f = |frame_info: UgoiraFrame| {
        let dimen = dimen.clone();
        move || {
            let file = &frame_info.file;
            let image = image::open(file.clone()).ok()?;
            let (width, height) = (image.width() as u16, image.height() as u16);
            dimen.get_or_init(|| (width, height));
            let mut frame = Frame::from_rgba_speed(width, height, &mut image.to_rgba8(), speed);
            frame.delay = (frame_info.delay / 10) as u16;
            frame.dispose = DisposalMethod::Background;
            Some(frame)
        }
    };
    for batch in metadata.chunks(8) {
        let stream: Vec<_> = batch.iter().map(|u| rt.spawn_blocking(f(u.clone()))).collect();
        for hnd in stream {
            let frame = hnd.await.unwrap().unwrap();
            LazyLock::force_mut(&mut encoder).write_frame(&frame).ok();
        }
    }
    Ok(())
}

#[unsafe(no_mangle)]
pub extern "C" fn encode_animated_image_unsafe(ptr: *const u8, len: i32) {
    let slice = unsafe { &*slice_from_raw_parts(ptr, len as usize) };
    static RT: OnceLock<Runtime> = OnceLock::new();
    let rt = RT.get_or_init(|| Runtime::new().unwrap());
    rt.block_on(encode_animated_image(slice, rt)).unwrap();
}
