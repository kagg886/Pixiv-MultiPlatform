#![feature(lazy_get)]
mod jvm;
use gif::{DisposalMethod, Encoder, Frame, Repeat};
use serde::Deserialize;
use std::{cell::LazyCell, collections::VecDeque, fs::File, ptr::slice_from_raw_parts, sync::{Arc, LazyLock, OnceLock}};
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

async fn encode_animated_image(src_buffer: &[u8], rt: &Runtime) {
    let GifEncodeRequest { metadata, speed, dstPath } = serde_cbor::from_slice(src_buffer).unwrap();
    let dst_file = File::create(dstPath).unwrap();
    let dimen = Arc::new(OnceLock::new());
    let mut encoder = LazyCell::new(|| {
        let (w, h) = *dimen.wait();
        let mut encoder = Encoder::new(dst_file, w, h, &[]).unwrap();
        encoder.set_repeat(Repeat::Infinite).unwrap();
        encoder
    });
    let mut c = |frame: Frame<'_>| {
        LazyCell::force_mut(&mut encoder).write_lzw_pre_encoded_frame(&frame).ok().unwrap()
    };
    let mut deque = VecDeque::new();
    for frame_info in metadata {
        let dimen = dimen.clone();
        deque.push_back(rt.spawn_blocking(move || {
            let file = frame_info.file;
            let image = image::open(file.clone()).unwrap();
            let (width, height) = (image.width() as u16, image.height() as u16);
            dimen.get_or_init(|| (width, height));
            let mut frame = Frame::from_rgba_speed(width, height, &mut image.to_rgba8(), speed);
            frame.delay = (frame_info.delay / 10) as u16;
            frame.dispose = DisposalMethod::Background;
            frame.make_lzw_pre_encoded();
            frame
        }));
        if deque.len() == 8 {
            let hnd = deque.pop_front().unwrap();
            c(hnd.await.unwrap())
        }
    }
    for hnd in deque {
        c(hnd.await.unwrap());
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn encode_animated_image_unsafe(ptr: *const u8, len: i32) {
    let slice = unsafe { &*slice_from_raw_parts(ptr, len as usize) };
    static RT: LazyLock<Runtime> = LazyLock::new(|| Runtime::new().unwrap());
    RT.block_on(encode_animated_image(slice, &RT));
}
