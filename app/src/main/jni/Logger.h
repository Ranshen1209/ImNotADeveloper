#pragma once

#include "android/log.h"

#define TAG "ImNotADeveloper"

#if defined(NDEBUG)
#define LOGD(...)
#else
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#endif

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
