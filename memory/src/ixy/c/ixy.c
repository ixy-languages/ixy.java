
#include "ixy.h"

// Common dependencies
#include <stdlib.h> // malloc, free
#include <string.h> // memcpy

// Linux dependencies
#ifdef __linux__
#include <unistd.h>       // getpagesize, sysconf, _SC_PAGESIZE, ftruncate, getpid, close, lseek
#include <stdio.h>        // FILE, fopen, perror, feof, fscanf, fclose, snprintf, unlink
#include <mntent.h>       // struct mntent, getmntent
#include <string.h>       // strcmp
#include <linux/limits.h> // PATH_MAX
#include <fcntl.h>        // open
#include <sys/types.h>    // O_CREAT, O_RDWR, S_IRWXU, SEEK_SET
#include <sys/mman.h>     // mmap, mlock, PROT_READ, PROT_WRITE, PROT_EXEC, MAP_SHARED, MAP_HUGETLB, MAP_LOCKED, MAP_NORESERVE, MAP_FAILED, munmap
#include <stdint.h>       // uint_t, uintptr_t

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

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1page_1size(const JNIEnv *env, const jclass klass) {
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
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1address_1size(const JNIEnv *env, const jclass klass) {
#ifdef _WIN32
	return sizeof(PVOID);
#else
	return sizeof(void *);
#endif
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1hugepage_1size(const JNIEnv *env, const jclass klass) {
#ifdef __linux__
// Phase 1: Find if the hugetlbfs is actually mounted
{
	FILE *fp = fopen("/etc/mtab", "r");
	if (fp == NULL) {
		perror("Error opening /etc/mtab");
		return -1;
	}
	char found = 0;
	while (!feof(fp)) {
		const struct mntent *mnt = getmntent(fp);
		if (mnt == NULL) {
			perror("Error reading mount entry");
			return -1;
		}
		if (strcmp(mnt->mnt_type, "hugetlbfs") == 0 && strcmp(mnt->mnt_fsname, "hugetlbfs") == 0 && strcmp(mnt->mnt_dir, "/mnt/huge") == 0) {
			found = 1;
			break;
		}
	}
	const int exit = fclose(fp);
	if (exit != 0) perror("Error closing /etc/mtab");
	if (!found) return -1;
}
// Phase 2: Find the size of the huge page using the pseudo-filesystem "proc"
{
	FILE *fp = fopen("/proc/meminfo", "r");
	if (fp == NULL) {
		perror("Could not open /proc/meminfo\n");
		return 0;
	}
	jlong hugepagesize = 0;
	char found = 0;
	while (!feof(fp)) {
		const char key[30] = {0};
		const char multiplier[3] = {0};
		const int items = fscanf(fp, "%s %d %3s\n", key, &hugepagesize, multiplier);
		if (items != 3 || strcmp(key, "Hugepagesize:") != 0) {
			fscanf(fp, "%*[^\n]");
		} else {
			switch (multiplier[0]) {
				case 'G':
					hugepagesize *= 1024*1024*1024;
					break;
				case 'M':
					hugepagesize *= 1024*1024;
					break;
				case 'k':
					hugepagesize *= 1024;
					break;
//				case 'B':
//					break;
			}
			found = 1;
			break;
		}
	}
	const int exit = fclose(fp);
	if (exit != 0) perror("Error closing /etc/mtab");
	return !found ? 0 : hugepagesize;
}
#elif _WIN32
	const SIZE_T size = GetLargePageMinimum();
	return (size <= 0) ? -1 : size;
#else
	return -1;
#endif
}

// Huge memory page id counter
static unsigned int hugepageid = 0;

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1allocate(JNIEnv *env, const jclass klass, const jlong size, const jboolean huge, jstring mnt) {
	// If no huge memory pages should be employed, then use the simple C memory allocation function
	if (!huge) return (jlong) malloc(size);

#ifdef __linux__
	// Get the prefix
	char *prefix = (*env)->GetStringUTFChars(env, mnt, NULL);

	// Build the path of a hugepage file
	char path[PATH_MAX];
	const unsigned int id = __atomic_fetch_add(&hugepageid, 1, __ATOMIC_SEQ_CST);
	snprintf(path, PATH_MAX, "%s/ixy-%d-%d", prefix, getpid(), id);

	// Release the string resource
	(*env)->ReleaseStringUTFChars(env, mnt, prefix);

	// Open the hugepage file
	const int fd = open(path, O_CREAT | O_RDWR, S_IRWXU);
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
	const void *virt_addr = mmap(NULL, size, PROT_READ | PROT_WRITE | PROT_EXEC, MAP_SHARED | MAP_HUGETLB | MAP_LOCKED | MAP_NORESERVE, fd, 0);
	if (virt_addr == MAP_FAILED) {
		perror("Error mmap-ing the hugepage file");
		return 0;
	}

	// Prevent the allocated memory to be swapped
	code = mlock(virt_addr, size);
	if (code != 0) perror("Error locking the allocated memory");

	// Close the hugepage file
	code = close(fd);
	if (code != 0) perror("Error closing the hugepage file");

	// Remove the file to avoid any other process from mapping it
	code = unlink(path);
	if (code != 0) perror("Error removing the hugepage file");

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
	if (!status || (error != ERROR_SUCCESS)) printf("AdjustTokenPrivileges error (%d)\n", GetLastError());

	// Allocate the memory
	const PVOID virt_addr = VirtualAlloc(NULL, size, MEM_LARGE_PAGES | MEM_RESERVE | MEM_COMMIT, PAGE_READWRITE);
	if (virt_addr == NULL) {
		printf("VirtualAlloc error (%d)\n", GetLastError());
		return 0;
	}

	// Lock the memory so that it cannot be swapped
	if (VirtualLock(virt_addr, size)) printf("VirtualLock error (%d)\n", GetLastError());

	// Disable the privilege for the process
	tp.PrivilegeCount -= 1;
	tp.Privileges[0].Attributes = 0;

	// Update the privileges
	status = AdjustTokenPrivileges(token, FALSE, &tp, 0, (PTOKEN_PRIVILEGES) NULL, 0);

	// It is possible for AdjustTokenPrivileges to return TRUE and still not succeed.
	// So always check for the last error value.
	error = GetLastError();
	if (!status || (error != ERROR_SUCCESS)) printf("AdjustTokenPrivileges error (%d)\n", GetLastError());

	// Close the handle
	if (!CloseHandle(token)) printf("CloseHandle error (%d)\n", GetLastError());

	// Return the address as a Java long
	return (jlong) virt_addr;
#else
	return 0;
#endif
}

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1free(const JNIEnv *env, const jclass klass, const jlong address, const jlong size, const jboolean huge) {
	// If no huge memory pages should be employed, then use the simple C memory allocation function
	if (!huge) {
		free((void *) address);
		return JNI_TRUE;
	}

#ifdef __linux__
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

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1virt2phys(const JNIEnv *env, const jclass klass, const jlong address) {
#ifdef __linux__
	// Open the pagemap file
	const int fd = open("/proc/self/pagemap", O_RDONLY);
	if(!fd) {
		perror("Could not open /proc/self/pagemap file");
		return 0;
	}

	// Get the page size to compute the page number and offset
	const jlong pagesize = sysconf(_SC_PAGESIZE);
	const jlong page = address / pagesize;

	// Move to the correct position
	const off_t pos = lseek(fd, ((uintptr_t) page) * sizeof(void *), SEEK_SET);
	if (pos == -1) {
		printf("Could not move the cursor to the correct position");
		return 0;
	}

	// Read the physical address
	jlong phy = 0;
	size_t code = read(fd, &phy, sizeof(void *));
	if (code == -1) {
		printf("Could not read the physical address");
		return 0;
	}

	// Close the pagemap file
	code = close(fd);
	if (code != 0) perror("Error closing the /proc/self/pagemap file");

	// Return the physical address with the fixed offset
	const uintptr_t offset = ((uintptr_t) address) & (pagesize - 1);
	return (jlong) ((phy & 0x7fffffffffffffULL)*pagesize + offset);
#else
	return 0;
#endif
}

////////////////////////////////////////////// UNSAFE RE-IMPLEMENTATIONS ///////////////////////////////////////////////

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jbyte *) address);
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((volatile jbyte *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	*((jbyte *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	*((volatile jbyte *) address) = value;
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1byte(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	jbyte *addr = (jbyte *) address;
	jbyte val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	volatile jbyte *addr = (volatile jbyte *) address;
	jbyte val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1byte(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	*((jbyte *) address) += value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	*((volatile jbyte *) address) += value;
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1byte(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	jbyte *addr = (jbyte *) address;
	jbyte val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	volatile jbyte *addr = (volatile jbyte *) address;
	jbyte val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1byte(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	jbyte *addr = (jbyte *) address;
	jbyte val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	volatile jbyte *addr = (volatile jbyte *) address;
	jbyte val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jshort *) address);
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((volatile jshort *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short(const JNIEnv *env, const jclass klass, const jlong address, jshort value) {
	*((jshort *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address, jshort value) {
	*((volatile jshort *) address) = value;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1short(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	jshort *addr = (jshort *) address;
	jshort val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	volatile jshort *addr = (volatile jshort *) address;
	jshort val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1short(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	*((jshort *) address) += value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	*((volatile jshort *) address) += value;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1short(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	jshort *addr = (jshort *) address;
	jshort val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	volatile jshort *addr = (volatile jshort *) address;
	jshort val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1short(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	jshort *addr = (jshort *) address;
	jshort val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jshort value) {
	volatile jshort *addr = (volatile jshort *) address;
	jshort val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jint *) address);
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((volatile jint *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	*((jint *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	*((volatile jint *) address) = value;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1int(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	jint *addr = (jint *) address;
	jint val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	volatile jint *addr = (volatile jint *) address;
	jint val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1int(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	*((jint *) address) += value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	*((volatile jint *) address) += value;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1int(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	jint *addr = (jint *) address;
	jint val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	volatile jint *addr = (volatile jint *) address;
	jint val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1int(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	jint *addr = (jint *) address;
	jint val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	volatile jint *addr = (volatile jint *) address;
	jint val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jlong *) address);
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((volatile jlong *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	*((jlong *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	*((volatile jlong *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get(JNIEnv *env, const jclass klass, const jlong src, const jint size, const jbyteArray dst, const jint offset) {
	jbyte *dstptr = (*env)->GetByteArrayElements(env, dst, NULL);
	memcpy((void *) (dstptr + offset), (void *) src, size);
	(*env)->ReleaseByteArrayElements(env, dst, dstptr, 0);
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1long(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	jlong *addr = (jlong *) address;
	jlong val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1put_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	volatile jlong *addr = (volatile jlong *) address;
	jlong val = *addr;
	*addr = value;
	return val;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1long(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	*((jlong *) address) += value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	*((volatile jlong *) address) += value;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1long(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	jlong *addr = (jlong *) address;
	jlong val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1and_1add_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	volatile jlong *addr = (volatile jlong *) address;
	jlong val = *addr;
	*addr += value;
	return val;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1long(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	jlong *addr = (jlong *) address;
	jlong val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1add_1and_1get_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	volatile jlong *addr = (volatile jlong *) address;
	jlong val = *addr + value;
	*addr = val;
	return val;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1volatile(JNIEnv *env, const jclass klass, const jlong src, jint size, const jbyteArray dst, const jint offset) {
	jbyte *dstptr = (*env)->GetByteArrayElements(env, dst, NULL);
	volatile const char *s = src;
	volatile char *d = dstptr + offset;
	while (size--) {
		*(d++) = *(s++);
	}
	(*env)->ReleaseByteArrayElements(env, dst, dstptr, 0);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put(JNIEnv *env, const jclass klass, const jlong dst, const jint size, const jbyteArray src, const jint offset) {
	jbyte *srcptr = (*env)->GetByteArrayElements(env, src, NULL);
	memcpy((void *) dst, (void *) (srcptr + offset), size);
	(*env)->ReleaseByteArrayElements(env, src, srcptr, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1volatile(JNIEnv *env, const jclass klass, const jlong dst, jint size, const jbyteArray src, const jint offset) {
	jbyte *srcptr = (*env)->GetByteArrayElements(env, src, NULL);
	volatile const char *s = (void *) (srcptr + offset);
	volatile char *d = (void *) dst;
	while (size--) {
		*(d++) = *(s++);
	}
	(*env)->ReleaseByteArrayElements(env, src, srcptr, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1copy(const JNIEnv *env, const jclass klass, const jlong src, const jint size, const jlong dst) {
	memcpy((void *) dst, (void *) src, size);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1copy_1volatile(const JNIEnv *env, const jclass klass, const jlong src, jint size, const jlong dst) {
	volatile const char *s = (void *) src;
	volatile char *d = (void *) dst;
	while (size--) {
		*(d++) = *(s++);
	}
}

#ifdef __cplusplus
}
#endif
