#pragma once

#include <jni.h> // JNIEXPORT, JNICALL, jint, JNIEnv, jclass, jlong, jboolean

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1pagesize(JNIEnv *, jclass);

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1addrsize(JNIEnv *, jclass);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1hugepage(JNIEnv *, jclass);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1allocate(JNIEnv *, jclass, jlong size, jboolean contiguous);

#ifdef __cplusplus
}
#endif
