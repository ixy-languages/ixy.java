#pragma once

#include <jni.h> // JNIEXPORT, JNICALL, jint, JNIEnv, jclass, jlong, jboolean, jbyte, jshort, jint

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1page_1size(JNIEnv *, jclass);

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1address_1size(JNIEnv *, jclass);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1hugepage_1size(JNIEnv *, jclass);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1allocate(JNIEnv *, jclass, jlong, jboolean, jboolean);

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1free(JNIEnv *, jclass, jlong, jlong, jboolean);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1virt2phys(JNIEnv *, jclass, jlong);

///////////////////////////////////////////////// UNSAFE REIMPLEMENTATIONS ///////////////////////////////////////////////

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte(JNIEnv *, jclass, jlong);

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte_1volatile(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte(JNIEnv *, jclass, jlong, jbyte);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte_1volatile(JNIEnv *, jclass, jlong, jbyte);

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short(JNIEnv *, jclass, jlong);

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short_1volatile(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short(JNIEnv *, jclass, jlong, jshort);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short_1volatile(JNIEnv *, jclass, jlong, jshort);

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int(JNIEnv *, jclass, jlong);

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int_1volatile(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int(JNIEnv *, jclass, jlong, jint);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int_1volatile(JNIEnv *, jclass, jlong, jint);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long(JNIEnv *, jclass, jlong);

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long_1volatile(JNIEnv *, jclass, jlong);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long(JNIEnv *, jclass, jlong, jlong);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long_1volatile(JNIEnv *, jclass, jlong, jlong);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1copy(JNIEnv *, jclass, jlong, jint, jbyteArray);

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1copy_1volatile(JNIEnv *, jclass, jlong, jint, jbyteArray);

#ifdef __cplusplus
}
#endif
