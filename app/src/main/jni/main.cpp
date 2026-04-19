#include <algorithm>
#include <cstdlib>
#include <cstring>
#include <string>
#include "LSPosed.h"
#include <sys/system_properties.h>
#include "PropKeys.h"
#include "Logger.h"
#include <jni.h>

int (*original_system_property_get)(const char *name, char *value);

int hooked_system_property_get(const char *name, char *value) {
    if (name == nullptr || value == nullptr) {
        return original_system_property_get(name, value);
    }

    std::string overrideValue;
    {
        std::lock_guard<std::mutex> lock(propOverridesMutex);
        auto newProp = propOverrides.find(name);
        if (newProp == propOverrides.end()) {
            return original_system_property_get(name, value);
        }
        overrideValue = newProp->second;
    }

    auto length = overrideValue.length();
    auto boundedLength = std::min(length, static_cast<size_t>(PROP_VALUE_MAX - 1));
    std::memcpy(value, overrideValue.c_str(), boundedLength);
    value[boundedLength] = '\0';
    return static_cast<int>(boundedLength);
}

const prop_info *(*original_system_property_find)(const char *name);

const prop_info *hooked_system_property_find(const char *name) {
    if (name == nullptr) {
        return original_system_property_find(name);
    }

    bool isOverridden;
    {
        std::lock_guard<std::mutex> lock(propOverridesMutex);
        isOverridden = propOverrides.find(name) != propOverrides.end();
    }
    if (isOverridden) {
        return nullptr;
    }
    return original_system_property_find(name);
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    HookFunType hookFunc = entries->hook_func;
    hookFunc((void *) __system_property_get, (void *) hooked_system_property_get,
             reinterpret_cast<void **>(&original_system_property_get));
    hookFunc((void *) __system_property_find, (void *) hooked_system_property_find,
             reinterpret_cast<void **>(&original_system_property_find));
    return [](const char *name, void *handle) {};
}