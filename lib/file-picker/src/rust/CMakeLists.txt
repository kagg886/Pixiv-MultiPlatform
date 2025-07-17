cmake_minimum_required(VERSION 3.14)
project(filepicker C)
include(FetchContent)

FetchContent_Declare(
        Corrosion
        GIT_REPOSITORY https://github.com/corrosion-rs/corrosion.git
        GIT_TAG v0.5.1
        GIT_SHALLOW 1
)

if(DEFINED ENV{CARGO_HOME})
        set(CARGO_HOME "$ENV{CARGO_HOME}")
else()
        set(CARGO_HOME "$ENV{HOME}/.cargo")
endif()

FetchContent_MakeAvailable(Corrosion)
corrosion_import_crate(MANIFEST_PATH Cargo.toml)
corrosion_set_features(filepicker FEATURES jvm)
corrosion_add_target_local_rustflags(filepicker "-Clink-args=-Wl,-z,max-page-size=16384")
corrosion_add_target_rustflags(filepicker "--remap-path-prefix=${CARGO_HOME}=cargo" "--remap-path-prefix=${CMAKE_CURRENT_SOURCE_DIR}=.")
