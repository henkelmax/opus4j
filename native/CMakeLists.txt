cmake_minimum_required(VERSION 3.12)

project(opus4j LANGUAGES C)

find_package(JNI REQUIRED)

include(FetchContent)
FetchContent_Declare(
        opus
        GIT_REPOSITORY https://github.com/xiph/opus.git
        GIT_TAG v1.5.2
)
FetchContent_MakeAvailable(opus)

add_library(opus4j SHARED
        src/encoder.c
        src/decoder.c
        src/exceptions.c
)

target_include_directories(opus4j PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${JNI_INCLUDE_DIRS}
        ${opus_SOURCE_DIR}/include
)

target_link_libraries(opus4j PRIVATE
        ${JNI_LIBRARIES}
        opus
)

if (UNIX)
    set_target_properties(opus4j PROPERTIES
            POSITION_INDEPENDENT_CODE ON
    )
endif ()

message(STATUS "C compiler executable: ${CMAKE_C_COMPILER}")
