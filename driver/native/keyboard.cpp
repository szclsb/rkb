#include "rkb/keyboard.h"

void (*callback)(const int, const bool);

LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
    if (nCode >= 0 && lParam != NULL) {
        auto key = ((LPKBDLLHOOKSTRUCT) lParam)->vkCode;
        // intercepts system key, so ALT+TAB and other system shortcuts won't work
        if (wParam == WM_KEYDOWN || wParam == WM_SYSKEYDOWN) {
            callback(key, false);
        } else if (wParam == WM_KEYUP || wParam == WM_SYSKEYUP) {
            callback(key, true);
        }
        return LRESULT(1);
    }
    return CallNextHookEx(nullptr, nCode, wParam, lParam);
}

void WINAPI rkb::invoke(int _vkCode, bool _up) {
    INPUT inputs[1] = {};
    ZeroMemory(inputs, sizeof(inputs));
    inputs[0].type = INPUT_KEYBOARD;
    inputs[0].ki.wVk = _vkCode;
    if (_up) {
        inputs[0].ki.dwFlags = KEYEVENTF_KEYUP;
    }
    SendInput(ARRAYSIZE(inputs), inputs, sizeof(INPUT));
}

void WINAPI rkb::scan(void (*_callback)(const int, const bool)) {
    callback = _callback;
    MSG msg;
    auto hook = SetWindowsHookExW(WH_KEYBOARD_LL, LowLevelKeyboardProc, nullptr, 0);
    while (GetMessage(&msg, nullptr, 0, 0)) {}
    UnhookWindowsHookEx(hook);
}

void WINAPI rkb::stop() {
    PostQuitMessage(0);
}
