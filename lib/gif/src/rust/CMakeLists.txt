cmake_minimum_required(VERSION 3.14)
project(gif_rust C)
include(FetchContent)

FetchContent_Declare(
        Corrosion
        GIT_REPOSITORY https://github.com/corrosion-rs/corrosion.git
        GIT_TAG v0.5.1
        GIT_SHALLOW 1
)

FetchContent_MakeAvailable(Corrosion)
corrosion_import_crate(MANIFEST_PATH Cargo.toml)

# Build and link our app's native lib
add_library(${PROJECT_NAME} SHARED kniwrapper.c)

if(NOT ANDROID)
    find_package(JNI)
    target_include_directories(${PROJECT_NAME} PUBLIC ${JNI_INCLUDE_DIRS})
endif()

target_link_libraries(${PROJECT_NAME} gif_encoder -Wl,-s)
