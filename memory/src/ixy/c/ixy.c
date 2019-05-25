#include "ixy.h"

// Linux dependencies
#ifdef __linux__
#include <unistd.h>       // getpagesize, sysconf, _SC_PAGESIZE, ftruncate, getpid, close
#include <stdio.h>        // FILE, fopen, perror, feof, fscanf, fclose, snprintf, unlink
#include <mntent.h>       // struct mntent, getmntent
#include <string.h>       // strcmp
#include <linux/limits.h> // PATH_MAX
#include <fcntl.h>        // open
#include <sys/types.h>    // O_CREAT, O_RDWR, S_IRWXU
#include <sys/mman.h>     // mmap, mlock, PROT_READ, PROT_WRITE, PROT_EXEC, MAP_SHARED, MAP_HUGETLB, MAP_LOCKED, MAP_NORESERVE, MAP_FAILED, munmap

// Windows dependencies
#elif _WIN32
#include <windows.h>           // Needed so that the compiler shuts up about "No Target Architecture"
#include <sysinfoapi.h>        // SYSTEM_INFO, GetSystemInfo
#include <winnt.h>             // PVOID, MEM_LARGE_PAGES, MEM_RESERVE, MEM_COMMIT, PAGE_READWRITE
#include <memoryapi.h>         // GetLargePageMinimum, VirtualAlloc, VirtualLock, VirtualFree
#include <basetsd.h>           // SIZE_T
#include <winnt.h>             // HANDLE, TOKEN_PRIVILEGES, TOKEN_ADJUST_PRIVILEGES, TOKEN_QUERY, SE_PRIVILEGE_ENABLED, TOKEN_PRIVILEGES
#include <processthreadsapi.h> // GetCurrentProcess, OpenProcessToken
#include <errhandlingapi.h>    // GetLastError
#include <winbase.h>           // LookupPrivilegeValue
#include <minwindef.h>         // SE_PRIVILEGE_ENABLED, BOOL, DWORD
#include <securitybaseapi.h>   // AdjustTokenPrivileges
#include <handleapi.h>         // CloseHandle
#endif

// Custom macros
#define HUGE_PAGE_BITS 21
#define HUGE_PAGE_SIZE (1 << HUGE_PAGE_BITS)

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1page_1size(JNIEnv *env, jclass klass) {
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
Java_de_tum_in_net_ixy_memory_Memory_c_1address_1size(JNIEnv *env, jclass klass) {
#ifdef _WIN32
    return sizeof(PVOID);
#else
    return sizeof(void *);
#endif
}

// Cached variable that will be usefull when allocating memory
static jlong hugepagesize = HUGE_PAGE_SIZE;

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1hugepage_1size(JNIEnv *env, jclass klass) {
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

// Hugepage id counter
static unsigned int hugepageid = 0;

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1allocate(JNIEnv *env, jclass klass, jlong size, jboolean contiguous) {

    // Skip if the pagesize is not a valid value
    if (hugepagesize <= 0) {
        return 0;
    }

	// Round the size to a multiple of the pagesize
    jlong mask = (hugepagesize - 1);
	if ((size & mask) != 0) {
		size = (size + hugepagesize) & ~mask;
	}

	// Skip if we cannot guarantee contiguity
    if (contiguous && size > hugepagesize) {
        return 0;
    }

#ifdef __linux__
	// Build the path of random hugepagea
    char path[PATH_MAX];
    unsigned int id = __atomic_fetch_add(&hugepageid, 1, __ATOMIC_SEQ_CST);
    snprintf(path, PATH_MAX, "/mnt/huge/ixy-%d-%d", getpid(), id);
    int fd = open(path, O_CREAT | O_RDWR, S_IRWXU);
    if(!fd) {
        perror("Could not create hugepage file");
        return 0;
    }

	// Make it as big as requested
    int code = ftruncate(fd, (off_t) size);
    if (code != 0) {
        perror("Error setting the size of the hugepage file");
        return 0;
    }

	// Map the hugepage file to memory
    // void *virt_addr = mmap(NULL, size, PROT_EXEC, MAP_PRIVATE | MAP_HUGETLB | MAP_LOCKED | MAP_NORESERVE, fd, 0);
    void *virt_addr = mmap(NULL, size, PROT_READ | PROT_WRITE | PROT_EXEC, MAP_SHARED | MAP_HUGETLB | MAP_LOCKED | MAP_NORESERVE, fd, 0);
    if (virt_addr == MAP_FAILED) {
        perror("Error mmap-ing the hugepage file");
        return 0;
    }

    // Prevent the allocated memory to be swapped
    code = mlock(virt_addr, size);
    if (code != 0) {
        perror("Error locking the allocated memory");
    }

    // Close the hugepage file
    code = close(fd);
    if (code != 0) {
        perror("Error closing the hugepage file");
    }

    code = unlink(path);
    if (code != 0) {
        perror("Error removing the hugepage file");
    }

	// Return the virtual address of the mapped hugepage
	return (jlong) virt_addr;
#elif _WIN32
    // Open process token
    HANDLE token;
    if (!OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &token)) {
        printf("OpenProcessToken error (%d)\n", GetLastError());
    }

    // Get the luid
    TOKEN_PRIVILEGES tp;
    if (!LookupPrivilegeValue(NULL, "SeLockMemoryPrivilege", &tp.Privileges[0].Luid)) {
        printf("LookupPrivilegeValue error (%d)\n", GetLastError());
    }

    // Enable the privilege for the process
    tp.PrivilegeCount = 1;
    tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;

    // Update the privileges
    BOOL status = AdjustTokenPrivileges(token, FALSE, &tp, 0, (PTOKEN_PRIVILEGES) NULL, 0);

    // It is possible for AdjustTokenPrivileges to return TRUE and still not succeed.
    // So always check for the last error value.
    DWORD error = GetLastError();
    if (!status || (error != ERROR_SUCCESS)) {
        printf("AdjustTokenPrivileges error (%d)\n", GetLastError());
    }

    // Allocate the memory
    PVOID virt_addr = VirtualAlloc(NULL, size, MEM_LARGE_PAGES | MEM_RESERVE | MEM_COMMIT, PAGE_READWRITE);
    if (virt_addr == NULL) {
        printf("VirtualAlloc error (%d)\n", GetLastError());
        return 0;
    }

    // Lock the memory so that it cannot be swapped
    if (VirtualLock(virt_addr, size)) {
        printf("VirtualLock error (%d)\n", GetLastError());
    }

    // Disable the privilege for the process
    tp.PrivilegeCount -= 1;
    tp.Privileges[0].Attributes = 0;

    // Update the privileges
    status = AdjustTokenPrivileges(token, FALSE, &tp, 0, (PTOKEN_PRIVILEGES) NULL, 0);

    // It is possible for AdjustTokenPrivileges to return TRUE and still not succeed.
    // So always check for the last error value.
    error = GetLastError();
    if (!status || (error != ERROR_SUCCESS)) {
        printf("AdjustTokenPrivileges error (%d)\n", GetLastError());
    }

    // Close the handle
    if (!CloseHandle(token)) {
        printf("CloseHandle error (%d)\n", GetLastError());
    }

    // Return the address as a Java long
    return (jlong) virt_addr;
#else
    return 0;
#endif
}

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1deallocate(JNIEnv *env, jclass klass, jlong address, jlong size) {

    // Skip if the pagesize is not a valid value
    if (hugepagesize <= 0) {
        return JNI_FALSE;
    }

	// Compute the mask to get the base address
    jlong mask = (hugepagesize - 1);
    if ((address & mask) != 0) {
		address &= ~mask;
	}

#ifdef __linux__

    // Round up the size as we did with the allocation
    if ((size & mask) != 0) {
		size = (size + hugepagesize) & ~mask;
	}

    // Deallocate the memory region
    int status = munmap((void *) address, size);
    if (status != 0) {
        perror("Error munmap-ing the hugepage file");
        return JNI_FALSE;
    }
    return JNI_TRUE;

#elif _WIN32
    // Deallocate the memory region
    if (!VirtualFree((void *) address, 0, MEM_RELEASE)) {
        printf("VirtualFree error (%d)\n", GetLastError());
        return JNI_FALSE;
    }
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

/////////////////////////////////////////////// UNSAFE REIMPLEMENTATIONS ///////////////////////////////////////////////

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1getByte(JNIEnv *env, jclass klass, jlong address) {
    return *((jbyte *) address);
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1getShort(JNIEnv *env, jclass klass, jlong address) {
    return *((jshort *) address);
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1getInt(JNIEnv *env, jclass klass, jlong address) {
    return *((jint *) address);
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1getLong(JNIEnv *env, jclass klass, jlong address) {
    return *((jlong *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1putByte(JNIEnv *env, jclass klass, jlong address, jbyte value) {
    *((jbyte *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1putShort(JNIEnv *env, jclass klass, jlong address, jshort value) {
    *((jshort *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1putInt(JNIEnv *env, jclass klass, jlong address, jint value) {
    *((jint *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_Memory_c_1putLong(JNIEnv *env, jclass klass, jlong address, jlong value) {
    *((jlong *) address) = value;
}

#ifdef __cplusplus
}
#endif
