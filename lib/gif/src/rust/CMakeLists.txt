cmake_minimum_required(VERSION 3.14)
include(FetchContent)

FetchContent_Declare(
        Corrosion
        GIT_REPOSITORY https://github.com/corrosion-rs/corrosion.git
        GIT_TAG v0.5.1
        GIT_SHALLOW 1
)

FetchContent_MakeAvailable(Corrosion)
corrosion_import_crate(MANIFEST_PATH Cargo.toml)
corrosion_set_features(gif_rust FEATURES jvm)
corrosion_add_target_local_rustflags(gif_rust "-Clink-args=-Wl,-z,max-page-size=16384")
