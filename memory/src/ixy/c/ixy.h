#pragma once

#include <jni.h> // JNIEXPORT, JNICALL, jint, JNIEnv, jclass, jlong, jboolean, jbyte, jshort, jint, jstring

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_page_size
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1page_1size(const JNIEnv *, const jclass);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_address_size
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1address_1size(const JNIEnv *, const jclass);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_hugepage_size
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1hugepage_1size(const JNIEnv *, const jclass);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_allocate
 * Signature: (JZLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1allocate(JNIEnv *, const jclass, const jlong, const jboolean, jstring);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_free
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1free(const JNIEnv *, const jclass, const jlong, const jlong, const jboolean);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_virt2phys
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1virt2phys(const JNIEnv *, const jclass, const jlong);

///////////////////////////////////////////////// UNSAFE REIMPLEMENTATIONS ///////////////////////////////////////////////

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_byte
 * Signature: (J)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_byte_volatile
 * Signature: (J)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte_1volatile(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_byte
 * Signature: (JB)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_byte_volatile
 * Signature: (JB)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte_1volatile(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_byte
 * Signature: (JB)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1byte(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_byte_volatile
 * Signature: (JB)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1byte_1volatile(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_byte
 * Signature: (JB)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1byte(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_byte_volatile
 * Signature: (JB)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1byte_1volatile(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_byte
 * Signature: (JB)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1byte(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_byte_volatile
 * Signature: (JB)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1byte_1volatile(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_byte
 * Signature: (JB)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1byte(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_byte_volatile
 * Signature: (JB)B
 */
JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1byte_1volatile(const JNIEnv *, const jclass, const jlong, const jbyte);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_short
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_short_volatile
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short_1volatile(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_short
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_short_volatile
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short_1volatile(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_short
 * Signature: (JS)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1short(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_short_volatile
 * Signature: (JS)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1short_1volatile(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_short
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1short(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_short_volatile
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1short_1volatile(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_short
 * Signature: (JS)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1short(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_short_volatile
 * Signature: (JS)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1short_1volatile(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_short
 * Signature: (JS)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1short(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_short_volatile
 * Signature: (JS)S
 */
JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1short_1volatile(const JNIEnv *, const jclass, const jlong, const jshort);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_int
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_int_volatile
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int_1volatile(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_int
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_int_volatile
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int_1volatile(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_int
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1int(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_int_volatile
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1int_1volatile(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_int
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1int(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_int_volatile
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1int_1volatile(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_int
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1int(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_int_volatile
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1int_1volatile(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_int
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1int(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_int_volatile
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1int_1volatile(const JNIEnv *, const jclass, const jlong, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_long
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_long_volatile
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long_1volatile(const JNIEnv *, const jclass, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_long
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_long_volatile
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long_1volatile(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_long
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1long(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_put_long_volatile
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1long_1volatile(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_long
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1long(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_long_volatile
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1long_1volatile(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_long
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1long(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_and_add_long_volatile
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1long_1volatile(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_long
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1long(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_add_and_get_long_volatile
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1long_1volatile(const JNIEnv *, const jclass, const jlong, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get
 * Signature: (JI[BI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get(JNIEnv *, const jclass, const jlong, const jint, const jbyteArray, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_get_volatile
 * Signature: (JI[BI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1volatile(JNIEnv *, const jclass, const jlong, const jint, const jbyteArray, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put
 * Signature: (JI[BI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put(JNIEnv *, const jclass, const jlong, const jint, const jbyteArray, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_put_volatile
 * Signature: (JI[BI)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1volatile(JNIEnv *, const jclass, const jlong, const jint, const jbyteArray, const jint);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_copy
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1copy(const JNIEnv *, const jclass, const jlong, const jint, const jlong);

/*
 * Class:     de_tum_in_net_ixy_memory_JniMemoryManager
 * Method:    c_copy_volatile
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1copy_1volatile(const JNIEnv *, const jclass, const jlong, const jint, const jlong);

#ifdef __cplusplus
}
#endif
