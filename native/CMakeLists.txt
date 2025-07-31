cmake_minimum_required(VERSION 3.12)

project(opus4j LANGUAGES C)

set(JDK_8_VERSION "jdk8u462-ga")
set(OPUS_VERSION "v1.5.2")

if (WIN32)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/refs/tags/${JDK_8_VERSION}/jdk/src/windows/javavm/export/jni_md.h")
elseif (APPLE)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/refs/tags/${JDK_8_VERSION}/jdk/src/macosx/javavm/export/jni_md.h")
elseif (UNIX)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/refs/tags/${JDK_8_VERSION}/jdk/src/solaris/javavm/export/jni_md.h")
else ()
    message(WARNING "Unknown OS, not compiling jni_md.h")
endif ()

file(DOWNLOAD
        "https://raw.githubusercontent.com/openjdk/jdk8u/refs/tags/${JDK_8_VERSION}/jdk/src/share/javavm/export/jni.h"
        ${CMAKE_BINARY_DIR}/jni_headers/jni.h
)

if (DEFINED JNI_MD_URL)
    file(DOWNLOAD
            ${JNI_MD_URL}
            ${CMAKE_BINARY_DIR}/jni_headers/jni_md.h
    )
endif ()

include(FetchContent)
FetchContent_Declare(
        opus
        GIT_REPOSITORY https://github.com/xiph/opus.git
        GIT_TAG ${OPUS_VERSION}
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
        ${CMAKE_BINARY_DIR}/jni_headers
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
