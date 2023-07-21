#pragma once

#ifdef RKB_EXPORTS
#define RKB_API  __declspec(dllexport)   // export DLL information
#else
#define RKB_API  __declspec(dllimport)   // import DLL information
#endif

#include <Windows.h>

extern "C" {
    namespace rkb {
        RKB_API void invoke(int _vkCode, bool _up);

        RKB_API void scan(void(*_callback)(const int, const bool));

        RKB_API void stop();
    }
}