#pragma once

#include <jni.h>
#include <map>
#include <mutex>
#include <string>
#include <utility>
#include "Logger.h"

static std::map<std::string, std::string> propOverrides;
static std::mutex propOverridesMutex;

extern "C"
JNIEXPORT void JNICALL
Java_io_github_auag0_imnotadeveloper_xposed_NativeFun_setProps(JNIEnv *env, jobject thiz, jobject props) {
    if (props == nullptr) {
        LOGE("setProps received null map");
        return;
    }

    jclass mapClass = nullptr;
    jobject entrySet = nullptr;
    jclass setClass = nullptr;
    jobject iterator = nullptr;
    jclass iteratorClass = nullptr;
    jclass entryClass = nullptr;
    jclass stringClass = nullptr;

    const auto deleteIfNotNull = [env](jobject reference) {
        if (reference != nullptr) {
            env->DeleteLocalRef(reference);
        }
    };
    const auto cleanup = [&] {
        deleteIfNotNull(stringClass);
        deleteIfNotNull(entryClass);
        deleteIfNotNull(iteratorClass);
        deleteIfNotNull(iterator);
        deleteIfNotNull(setClass);
        deleteIfNotNull(entrySet);
        deleteIfNotNull(mapClass);
    };
    const auto hasPendingException = [env](const char *message) {
        if (!env->ExceptionCheck()) {
            return false;
        }
        LOGE("%s", message);
        return true;
    };
    const auto throwInvalidMap = [&] {
        jclass illegalArgumentClass = env->FindClass("java/lang/IllegalArgumentException");
        if (illegalArgumentClass != nullptr) {
            env->ThrowNew(illegalArgumentClass, "setProps expects Map<String, String>");
            env->DeleteLocalRef(illegalArgumentClass);
        }
    };

    mapClass = env->FindClass("java/util/Map");
    if (mapClass == nullptr || hasPendingException("Failed to find Map class")) {
        cleanup();
        return;
    }
    jmethodID entrySetMethod = env->GetMethodID(mapClass, "entrySet", "()Ljava/util/Set;");
    if (entrySetMethod == nullptr || hasPendingException("Failed to find Map.entrySet")) {
        cleanup();
        return;
    }
    entrySet = env->CallObjectMethod(props, entrySetMethod);
    if (entrySet == nullptr || hasPendingException("Failed to read Map.entrySet")) {
        cleanup();
        return;
    }

    setClass = env->FindClass("java/util/Set");
    if (setClass == nullptr || hasPendingException("Failed to find Set class")) {
        cleanup();
        return;
    }
    jmethodID iteratorMethod = env->GetMethodID(setClass, "iterator", "()Ljava/util/Iterator;");
    if (iteratorMethod == nullptr || hasPendingException("Failed to find Set.iterator")) {
        cleanup();
        return;
    }
    iterator = env->CallObjectMethod(entrySet, iteratorMethod);
    if (iterator == nullptr || hasPendingException("Failed to create iterator")) {
        cleanup();
        return;
    }

    iteratorClass = env->FindClass("java/util/Iterator");
    if (iteratorClass == nullptr || hasPendingException("Failed to find Iterator class")) {
        cleanup();
        return;
    }
    jmethodID hasNextMethod = env->GetMethodID(iteratorClass, "hasNext", "()Z");
    jmethodID nextMethod = env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;");
    if (hasNextMethod == nullptr || nextMethod == nullptr || hasPendingException("Failed to find Iterator methods")) {
        cleanup();
        return;
    }

    entryClass = env->FindClass("java/util/Map$Entry");
    if (entryClass == nullptr || hasPendingException("Failed to find Map.Entry class")) {
        cleanup();
        return;
    }
    jmethodID getKeyMethod = env->GetMethodID(entryClass, "getKey", "()Ljava/lang/Object;");
    jmethodID getValueMethod = env->GetMethodID(entryClass, "getValue", "()Ljava/lang/Object;");
    if (getKeyMethod == nullptr || getValueMethod == nullptr || hasPendingException("Failed to find Map.Entry methods")) {
        cleanup();
        return;
    }

    stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr || hasPendingException("Failed to find String class")) {
        cleanup();
        return;
    }

    std::map<std::string, std::string> updatedOverrides;
    while (env->CallBooleanMethod(iterator, hasNextMethod)) {
        if (hasPendingException("Iterator.hasNext failed")) {
            cleanup();
            return;
        }

        jobject entry = env->CallObjectMethod(iterator, nextMethod);
        if (entry == nullptr || hasPendingException("Iterator.next failed")) {
            deleteIfNotNull(entry);
            cleanup();
            return;
        }

        jobject keyObj = env->CallObjectMethod(entry, getKeyMethod);
        jobject valueObj = env->CallObjectMethod(entry, getValueMethod);
        if (hasPendingException("Failed to read map entry")) {
            deleteIfNotNull(keyObj);
            deleteIfNotNull(valueObj);
            deleteIfNotNull(entry);
            cleanup();
            return;
        }

        if (keyObj == nullptr || valueObj == nullptr ||
            !env->IsInstanceOf(keyObj, stringClass) ||
            !env->IsInstanceOf(valueObj, stringClass)) {
            deleteIfNotNull(keyObj);
            deleteIfNotNull(valueObj);
            deleteIfNotNull(entry);
            throwInvalidMap();
            cleanup();
            return;
        }

        auto keyString = reinterpret_cast<jstring>(keyObj);
        auto valueString = reinterpret_cast<jstring>(valueObj);
        const char *keyChars = env->GetStringUTFChars(keyString, nullptr);
        const char *valueChars = env->GetStringUTFChars(valueString, nullptr);
        if (keyChars == nullptr || valueChars == nullptr || hasPendingException("Failed to copy string chars")) {
            if (keyChars != nullptr) {
                env->ReleaseStringUTFChars(keyString, keyChars);
            }
            if (valueChars != nullptr) {
                env->ReleaseStringUTFChars(valueString, valueChars);
            }
            deleteIfNotNull(keyObj);
            deleteIfNotNull(valueObj);
            deleteIfNotNull(entry);
            cleanup();
            return;
        }

        updatedOverrides[std::string(keyChars)] = std::string(valueChars);

        env->ReleaseStringUTFChars(keyString, keyChars);
        env->ReleaseStringUTFChars(valueString, valueChars);
        deleteIfNotNull(keyObj);
        deleteIfNotNull(valueObj);
        deleteIfNotNull(entry);
    }

    {
        std::lock_guard<std::mutex> lock(propOverridesMutex);
        propOverrides = std::move(updatedOverrides);
    }
    cleanup();
}