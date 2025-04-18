mod jvm;
use color_quant::NeuQuant;
use gif::{DisposalMethod, Encoder, Frame as GifFrame, Repeat};
use image::{RgbaImage, open};
use serde::Deserialize;
use std::{borrow::Cow, collections::VecDeque, fs::File, ptr::slice_from_raw_parts, sync::{Arc, LazyLock}};
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

fn mutate_frame_transparent(frame: &mut RgbaImage) -> Option<[u8; 4]> {
    let mut transparent = None;
    for pix in frame.chunks_exact_mut(4) {
        if pix[3] != 0 {
            pix[3] = 0xFF;
        } else {
            transparent = Some([pix[0], pix[1], pix[2], pix[3]]);
        }
    }
    transparent
}

fn generate_frame_with_global_palette(frame: RgbaImage, nq: &NeuQuant, delay: u64, transparent: Option<[u8; 4]>) -> GifFrame<'static> {
    let (w, h) = u32_dimen_to_u16(frame.dimensions());
    let mut frame = GifFrame {
        delay: (delay / 10) as u16,
        dispose: DisposalMethod::Background,
        transparent: transparent.map(|t| nq.index_of(&t) as u8),
        needs_user_input: false,
        top: 0,
        left: 0,
        width: w,
        height: h,
        interlaced: false,
        palette: None,
        buffer: Cow::Owned(frame.chunks_exact(4).map(|pix| nq.index_of(pix) as u8).collect()),
    };
    frame.make_lzw_pre_encoded();
    frame
}

async fn encode_animated_image(src_buffer: &'static [u8], rt: &Runtime) {
    let GifEncodeRequest { mut metadata, speed, dstPath } = serde_cbor::from_slice(src_buffer).unwrap();
    let Frame { file, delay } = metadata.remove(0);
    let mut first_image = open(file).unwrap().to_rgba8();
    let transparent = mutate_frame_transparent(&mut first_image);
    let (w, h) = u32_dimen_to_u16(first_image.dimensions());
    let nq = Arc::new(NeuQuant::new(speed, 255, &first_image));
    let dst_file = File::create(dstPath).unwrap();
    let mut encoder = Encoder::new(dst_file, w, h, &nq.color_map_rgb()).unwrap();
    encoder.set_repeat(Repeat::Infinite).unwrap();
    let mut writer = |frame: GifFrame<'_>| encoder.write_lzw_pre_encoded_frame(&frame).unwrap();
    let first_frame = generate_frame_with_global_palette(first_image, &nq, delay, transparent);
    writer(first_frame);
    let mut deque = VecDeque::new();
    for frame_info in metadata {
        let nq = nq.clone();
        deque.push_back(rt.spawn_blocking(move || {
            let file = frame_info.file;
            let mut image = open(file).unwrap().into_rgba8();
            let transparent = mutate_frame_transparent(&mut image);
            generate_frame_with_global_palette(image, &nq, frame_info.delay, transparent)
        }));
        if deque.len() == 8 {
            let hnd = deque.pop_front().unwrap();
            writer(hnd.await.unwrap())
        }
    }
    for hnd in deque {
        writer(hnd.await.unwrap());
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn encode_animated_image_unsafe(ptr: *const u8, len: i32) {
    let slice = unsafe { &*slice_from_raw_parts(ptr, len as usize) };
    static RT: LazyLock<Runtime> = LazyLock::new(|| Runtime::new().unwrap());
    RT.block_on(encode_animated_image(slice, &RT));
}
