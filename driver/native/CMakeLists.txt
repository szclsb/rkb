cmake_minimum_required(VERSION 3.22)
project(rkb_native)

set(CMAKE_CXX_STANDARD 20)

set(SOURCE_FILES
        keyboard.cpp
        )

add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})
target_include_directories(${PROJECT_NAME} PUBLIC include)
