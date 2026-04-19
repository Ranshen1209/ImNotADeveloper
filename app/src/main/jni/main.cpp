#include <algorithm>
#include <cstdlib>
#include <cstring>
#include "LSPosed.h"
#include <sys/system_properties.h>
#include "PropKeys.h"
#include "Logger.h"
#include <jni.h>

int (*original_system_property_get)(const char *name, char *value);

int hooked_system_property_get(const char *name, char *value) {
    LOGD("__system_property_get: %s", name);
    auto newProp = propOverrides.find(name);
    if (newProp != propOverrides.end()) {
        auto length = std::strlen(newProp->second);
        auto boundedLength = std::min(length, static_cast<size_t>(PROP_VALUE_MAX - 1));
        std::memcpy(value, newProp->second, boundedLength);
        value[boundedLength] = '\0';
        return static_cast<int>(boundedLength);
    }
    return original_system_property_get(name, value);
}

const prop_info *(*original_system_property_find)(const char *name);

const prop_info *hooked_system_property_find(const char *name) {
    // idk, it loop and freeze
    //LOGD("__system_property_find: %s", name);
    auto newProp = propOverrides.find(name);
    if (newProp != propOverrides.end()) {
        return nullptr;
    }
    return original_system_property_find(name);
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    LOGD("native_init");
    HookFunType hookFunc = entries->hook_func;
    hookFunc((void *) __system_property_get, (void *) hooked_system_property_get,
             reinterpret_cast<void **>(&original_system_property_get));
    hookFunc((void *) __system_property_find, (void *) hooked_system_property_find,
             reinterpret_cast<void **>(&original_system_property_find));
    return [](const char *name, void *handle) {};
}