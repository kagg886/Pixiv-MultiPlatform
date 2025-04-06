mod jvm;
use gif::{DisposalMethod, Encoder, Frame as GifFrame, Repeat};
use image::{GenericImageView, image_dimensions, open};
use serde::Deserialize;
use std::{collections::VecDeque, fs::File, ptr::slice_from_raw_parts, sync::LazyLock};
use tokio::runtime::Runtime;

#[derive(Deserialize, Clone)]
#[allow(non_snake_case)]
struct Frame<'a> {
    file: &'a str,
    delay: u64,
}

#[derive(Deserialize)]
#[allow(non_snake_case)]
struct GifEncodeRequest<'a> {
    #[serde(borrow)]
    metadata: Vec<Frame<'a>>,
    speed: i32,
    dstPath: &'a str,
}

fn u32_dimen_to_u16(input: (u32, u32)) -> (u16, u16) {
    (input.0 as u16, input.1 as u16)
}

async fn encode_animated_image(src_buffer: &'static [u8], rt: &Runtime) {
    let GifEncodeRequest { metadata, speed, dstPath } = serde_cbor::from_slice(src_buffer).unwrap();
    let first_frame_path = metadata[0].file;
    let (w, h) = u32_dimen_to_u16(image_dimensions(first_frame_path).unwrap());
    let dst_file = File::create(dstPath).unwrap();
    let mut encoder = Encoder::new(dst_file, w, h, &[]).unwrap();
    encoder.set_repeat(Repeat::Infinite).unwrap();
    let mut c = |frame: GifFrame<'_>| encoder.write_lzw_pre_encoded_frame(&frame).unwrap();
    let mut deque = VecDeque::new();
    for frame_info in metadata {
        deque.push_back(rt.spawn_blocking(move || {
            let file = frame_info.file;
            let image = open(file).unwrap();
            let (w, h) = u32_dimen_to_u16(image.dimensions());
            let mut frame = GifFrame::from_rgba_speed(w, h, &mut image.to_rgba8(), speed);
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
