#pragma once

#include <jni.h> // JNIEXPORT, JNICALL, jint, JNIEnv, jclass, jlong, jboolean, jbyte, jshort, jint

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
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1allocate(JNIEnv *, jclass, jlong, jboolean);

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1deallocate(JNIEnv *, jclass, jlong, jlong);

/////////////////////////////////////////////// UNSAFE REIMPLEMENTATIONS ///////////////////////////////////////////////

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1getByte(JNIEnv *, jclass, jlong);

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1getShort(JNIEnv *, jclass, jlong);

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1getInt(JNIEnv *, jclass, jlong);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1getLong(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1putByte(JNIEnv *, jclass, jlong, jbyte);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1putShort(JNIEnv *, jclass, jlong, jshort);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1putInt(JNIEnv *, jclass, jlong, jint);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1putLong(JNIEnv *, jclass, jlong, jlong);

#ifdef __cplusplus
}
#endif
