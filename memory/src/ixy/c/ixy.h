#pragma once

#include <jni.h> // JNIEXPORT, JNICALL, jint, JNIEnv, jclass

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1pagesize(JNIEnv *, jclass);

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1addrsize(JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
