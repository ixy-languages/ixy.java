#include "ixy.h"

// Linux dependencies
#ifdef __linux__
#include <unistd.h> // getpagesize, sysconf, _SC_PAGESIZE

// Windows dependencies
#elif _WIN32
#include <windows.h>    // Needed so that the compiler shuts up about "No Target Architecture"
#include <sysinfoapi.h> // SYSTEM_INFO, GetSystemInfo
#include <winnt.h>      // PVOID
#endif

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1pagesize(JNIEnv *env, jclass class) {
#ifdef __linux__
    return sysconf(_SC_PAGESIZE);
#elif _WIN32
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    return si.dwPageSize;
#else
    return 0;
#endif
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1addrsize(JNIEnv *env, jclass class) {
#ifdef _WIN32
    return sizeof(PVOID);
#else
    return sizeof(void *);
#endif
}

#ifdef __cplusplus
}
#endif
