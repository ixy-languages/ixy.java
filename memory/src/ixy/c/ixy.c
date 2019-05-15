#include "ixy.h"

// Common imports
#include <string.h> // strcmp

// Linux dependencies
#ifdef __linux__
#include <unistd.h> // getpagesize, sysconf, _SC_PAGESIZE
#include <stdio.h>  // FILE, fopen, perror, feof, fscanf, fclose
#include <mntent.h> // struct mntent, getmntent

// Windows dependencies
#elif _WIN32
#include <windows.h>    // Needed so that the compiler shuts up about "No Target Architecture"
#include <sysinfoapi.h> // SYSTEM_INFO, GetSystemInfo
#include <winnt.h>      // PVOID
#include <memoryapi.h>  // GetLargePageMinimum
#include <basetsd.h>    // SIZE_T
#endif

// Custom macros
#define HUGE_PAGE_BITS 21
#define HUGE_PAGE_SIZE (1 << HUGE_PAGE_BITS)

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

// Cached variable that will be usefull when allocating memory
static jlong hugepagesize = HUGE_PAGE_SIZE;

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_MemoryUtils_c_1hugepage(JNIEnv *env, jclass class) {
#ifdef __linux__
// Phase 1: Find if the hugetlbfs is actually mounted
{
    FILE *fp = fopen("/etc/mtab", "r");
    if (fp == NULL) {
        perror("Error opening /etc/mtab");
        hugepagesize = -1;
        return hugepagesize;
    }
    char found = 0;
    while (!feof(fp)) {
        struct mntent *mnt = getmntent(fp);
        if (mnt == NULL) {
            perror("Error reading mount entry");
            hugepagesize = -1;
            return hugepagesize;
        }
        if (strcmp(mnt->mnt_type, "hugetlbfs") == 0 && strcmp(mnt->mnt_fsname, "hugetlbfs") == 0 && strcmp(mnt->mnt_dir, "/mnt/huge") == 0) {
            found = 1;
            break;
        }
    }
    int exit = fclose(fp);
    if (exit != 0) {
        perror("Error closing /etc/mtab");
    }
    if (!found) {
        hugepagesize = -1;
        return hugepagesize;
    }
}
// Phase 2: Find the size of the huge page using the pseudo-filesystem "proc"
{
    FILE *fp = fopen("/proc/meminfo", "r");
    if (fp == NULL) {
        perror("Could not open /proc/meminfo\n");
        hugepagesize = 0;
        return hugepagesize;
    }
    char found = 0;
    while (!feof(fp)) {
        char key[30] = {0};
        char multiplier[3] = {0};
        int items = fscanf(fp, "%s %d %3s\n", key, &hugepagesize, multiplier);
        if (items != 3 || strcmp(key, "Hugepagesize:") != 0) {
            fscanf(fp, "%*[^\n]");
        } else {
            switch (multiplier[0]) {
                case 'G':
                    hugepagesize *= 1024;
                case 'M':
                    hugepagesize *= 1024;
                case 'k':
                    hugepagesize *= 1024;
                case 'B':
                    break;
            }
            found = 1;
            break;
        }
    }
    int exit = fclose(fp);
    if (exit != 0) {
        perror("Error closing /etc/mtab");
    }
    if (!found) {
        hugepagesize = 0;
    }
    return hugepagesize;
}
#elif _WIN32
    SIZE_T size = GetLargePageMinimum();
    hugepagesize = (size <= 0) ? -1 : size;
    return hugepagesize;
#else
    return -1;
#endif
}

#ifdef __cplusplus
}
#endif
