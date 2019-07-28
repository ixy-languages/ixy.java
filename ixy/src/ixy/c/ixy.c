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
#include <sys/stat.h>     // struct stat, fstat
#include <stdint.h>       // uint_t, uintptr_t
#endif

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_FastestMemoryManager_c_1is_1valid(const JNIEnv *env, const jclass klass) {
#ifdef __linux__
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

// Huge memory page id counter
static unsigned int hugepageid = 0;

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_FastestMemoryManager_c_1allocate(JNIEnv *env, const jclass klass, const jlong size, jstring mnt) {
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
	const void *virt_addr = mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_SHARED /*| MAP_HUGETLB /*| MAP_LOCKED | MAP_NORESERVE*/, fd, 0);
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
#else
	return 0;
#endif
}

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_FastestMemoryManager_c_1free(const JNIEnv *env, const jclass klass, const jlong address, const jlong size) {
#ifdef __linux__
	// Deallocate the memory region
	int status = munmap((void *) address, size);
	if (status != 0) {
		perror("Error munmap-ing the hugepage file");
		return JNI_FALSE;
	}
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

// Cached fields of the class java.io.FileDescriptor
static struct CachedFields {
    jclass fileDescriptorClass;
    jmethodID fileDescriptorCtor;
    jfieldID descriptorField;
} gCachedFields;

// Caches the fields from the struct "gCachedFields"
jboolean cacheFields(JNIEnv* env) {
	if (gCachedFields.fileDescriptorClass == NULL) {
		gCachedFields.fileDescriptorClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/io/FileDescriptor"));
		if (gCachedFields.fileDescriptorClass == NULL) {
			return JNI_FALSE;
		}
	}
	if (gCachedFields.fileDescriptorCtor == NULL) {
		gCachedFields.fileDescriptorCtor = (*env)->GetMethodID(env, gCachedFields.fileDescriptorClass, "<init>", "()V");
		if (gCachedFields.fileDescriptorCtor == NULL) {
			return JNI_FALSE;
		}
	}
	if (gCachedFields.descriptorField == NULL) {
		gCachedFields.descriptorField = (*env)->GetFieldID(env, gCachedFields.fileDescriptorClass, "fd", "I");
		if (gCachedFields.descriptorField == NULL) {
			return JNI_FALSE;
		}
	}
	return JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_FastestMemoryManager_c_1mmap(JNIEnv *env, const jclass klass, const jobject jfd, const jlong size, const jboolean huge, const jboolean lock) {
#ifdef __linux__
	// Initial values to write
	void *map = MAP_FAILED;
	int fd = -1;

	// Cache the fields of the class and get the file descriptor
	if (cacheFields(env) == JNI_FALSE) return 0L;
	fd = (*env)->GetIntField(env, jfd, gCachedFields.descriptorField);

	// Map the file
	int flags = MAP_SHARED;
	if (huge) flags |= MAP_HUGETLB;
	if (lock) flags |= MAP_NORESERVE;

	map = mmap(NULL, size, PROT_READ | PROT_WRITE, flags, fd, 0);
	if (map == MAP_FAILED) {
		perror("Error memory mapping file");
		return 0;
	}

	// Lock the file
	int code = mlock(map, size);
	if (code != 0) {
		perror("Error locking the mapped memory");
	}

	// Return the memory mapping
	return (jlong) map;
#else
	return 0;
#endif
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_FastestMemoryManager_c_1munmap(JNIEnv *env, const jclass klass, const jlong addr, const jlong size) {
#ifdef __linux__
	// Destroy the mapping
	int code = munmap((void *) addr, (size_t) size);
	if (code != 0) {
		perror("Error unmapping file");
	}
#endif
}

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_SmartUnsafeMemoryManager_c_1is_1valid(const JNIEnv *env, const jclass klass) {
#ifdef __linux__
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_SmartUnsafeMemoryManager_c_1allocate(JNIEnv *env, const jclass klass, const jlong size, const jboolean huge, const jboolean lock, jstring mnt) {
	// If no huge memory pages should be employed, then use the simple C memory allocation function
	if (!huge) {
		void *addr = malloc((size_t) size);
		if (addr == NULL) {
			perror("Error allocating memory");
			fflush(stderr);
			return 0;
		}
		if (lock && mlock(addr, (size_t) size) != 0) {
			perror("Error locking memory");
			fflush(stderr);
			return 0;
		}
		return (jlong) addr;
	}

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
	if(fd == -1) {
		perror("Could not create hugepage file");
		fflush(stderr);
		return 0;
	}

	// Make it as big as requested
	if (ftruncate(fd, (off_t) size) != 0) {
		perror("Error setting the size of the hugepage file");
		fflush(stderr);
		return 0;
	}

	// Map the hugepage file to memory
	int flags = MAP_SHARED;
	if (lock) flags |= MAP_LOCKED;
	const void *virt_addr = mmap(NULL, size, PROT_READ | PROT_WRITE, flags, fd, 0);
	if (virt_addr == MAP_FAILED) {
		perror("Error mmap-ing the hugepage file");
		fflush(stderr);
		return 0;
	}

	// Prevent the allocated memory to be swapped
	if (lock && mlock(virt_addr, size) != 0) {
		perror("Error locking the allocated memory");
		fflush(stderr);
		return 0;
	}

	// Close the hugepage file
	if (close(fd) != 0) {
		perror("Error closing the hugepage file");
		fflush(stderr);
	}

	// Remove the file to avoid any other process from mapping it
	if (unlink(path) != 0) {
		perror("Error removing the hugepage file");
		fflush(stderr);
	}

	// Return the virtual address of the mapped hugepage
	return (jlong) virt_addr;
#else
	return 0;
#endif
}

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_SmartUnsafeMemoryManager_c_1free(const JNIEnv *env, const jclass klass, const jlong address, const jlong size, const jboolean huge, const jboolean lock) {
	// If no huge memory pages should be employed, then use the simple C memory allocation function
	if (!huge) {
		free((void *) address);
		return JNI_TRUE;
	}

#ifdef __linux__
	// Deallocate the memory region
	if (munmap((void *) address, size) != 0) {
		perror("Error munmap-ing the hugepage file");
		fflush(stderr);
		return JNI_FALSE;
	}
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_SmartUnsafeMemoryManager_c_1mmap(JNIEnv *env, const jclass klass, const jobject jfd, const jlong size, const jboolean huge, const jboolean lock) {
#ifdef __linux__
	// Cache the fields of the class and get the file descriptor
	if (cacheFields(env) == JNI_FALSE) return 0;
	const int fd = (*env)->GetIntField(env, jfd, gCachedFields.descriptorField);

	// Map the file
	int flags = MAP_SHARED;
	if (huge) flags |= MAP_HUGETLB;
	if (lock) flags |= MAP_NORESERVE | MAP_LOCKED;
	void *map = mmap(NULL, size, PROT_READ | PROT_WRITE, flags, fd, 0);
	if (map == MAP_FAILED) {
		perror("Error memory mapping file");
		fflush(stderr);
		printf(" * File descriptor: %d\n", fd);
		printf(" * Size: %d (mod page = %d; mod huge = %d)\n",
				size,
				size % sysconf(_SC_PAGESIZE),
				size % Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1hugepage_1size(env, klass));
		printf(" * Huge: %s\n", huge ? "true" : "false");
		printf(" * Lock: %s\n", lock ? "true" : "false");
		fflush(stdout);
		return 0;
	}

	// Lock the file
	if (lock && mlock(map, size) != 0) {
		perror("Error locking the mapped memory");
		fflush(stderr);
	}

	// Return the memory mapping
	return (jlong) map;
#else
	return 0;
#endif
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_SmartUnsafeMemoryManager_c_1munmap(JNIEnv *env, const jclass klass, const jlong address, const jlong size, const jboolean huge, const jboolean lock) {
#ifdef __linux__
	if (munmap((void *) address, (size_t) size) != 0) {
		perror("Error unmapping file");
		fflush(stderr);
	}
#endif
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1is_1valid(const JNIEnv *env, const jclass klass) {
#ifdef __linux__
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1page_1size(const JNIEnv *env, const jclass klass) {
#ifdef __linux__
	return sysconf(_SC_PAGESIZE);
#else
	return 0;
#endif
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1address_1size(const JNIEnv *env, const jclass klass) {
	return sizeof(void *);
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1hugepage_1size(const JNIEnv *env, const jclass klass) {
#ifdef __linux__
// Phase 1: Find if the hugetlbfs is actually mounted
{
	FILE *fp = fopen("/etc/mtab", "r");
	if (fp == NULL) {
		perror("Error opening /etc/mtab");
		fflush(stderr);
		return -1;
	}
	char found = 0;
	while (!feof(fp)) {
		const struct mntent *mnt = getmntent(fp);
		if (mnt == NULL) {
			perror("Error reading mount entry");
			fflush(stderr);
			return -1;
		}
		if (strcmp(mnt->mnt_type, "hugetlbfs") == 0 && strcmp(mnt->mnt_fsname, "hugetlbfs") == 0 && strcmp(mnt->mnt_dir, "/mnt/huge") == 0) {
			found = 1;
			break;
		}
	}
	if (fclose(fp) != 0) {
		perror("Error closing /etc/mtab");
		fflush(stderr);
	}
	if (!found) return -1;
}

// Phase 2: Find the size of the huge page using the pseudo-filesystem "proc"
{
	FILE *fp = fopen("/proc/meminfo", "r");
	if (fp == NULL) {
		perror("Could not open /proc/meminfo\n");
		fflush(stderr);
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
	if (fclose(fp) != 0) {
		perror("Error closing /etc/mtab");
		fflush(stderr);
	}
	return !found ? 0 : hugepagesize;
}
#else
	return -1;
#endif
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1allocate(JNIEnv *env, const jclass klass, const jlong size, const jboolean huge, const jboolean lock, jstring mnt) {
	// If no huge memory pages should be employed, then use the simple C memory allocation function
	if (!huge) {
		void *addr = malloc((size_t) size);
		if (addr == NULL) {
			perror("Error allocating memory");
			fflush(stderr);
			return 0;
		}
		if (lock && mlock(addr, (size_t) size) != 0) {
			perror("Error locking memory");
			fflush(stderr);
			return 0;
		}
		return (jlong) addr;
	}

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
	if(fd == -1) {
		perror("Could not create hugepage file");
		fflush(stderr);
		return 0;
	}

	// Make it as big as requested
	if (ftruncate(fd, (off_t) size) != 0) {
		perror("Error setting the size of the hugepage file");
		fflush(stderr);
		return 0;
	}

	// Map the hugepage file to memory
	int flags = MAP_SHARED;
	if (lock) flags |= MAP_LOCKED;
	const void *virt_addr = mmap(NULL, size, PROT_READ | PROT_WRITE, flags, fd, 0);
	if (virt_addr == MAP_FAILED) {
		perror("Error mmap-ing the hugepage file");
		fflush(stderr);
		return 0;
	}

	// Prevent the allocated memory to be swapped
	if (lock && mlock(virt_addr, size) != 0) {
		perror("Error locking the allocated memory");
		fflush(stderr);
		return 0;
	}

	// Close the hugepage file
	if (close(fd) != 0) {
		perror("Error closing the hugepage file");
		fflush(stderr);
	}

	// Remove the file to avoid any other process from mapping it
	if (unlink(path) != 0) {
		perror("Error removing the hugepage file");
		fflush(stderr);
	}

	// Return the virtual address of the mapped hugepage
	return (jlong) virt_addr;
#else
	return 0;
#endif
}

JNIEXPORT jboolean JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1free(const JNIEnv *env, const jclass klass, const jlong address, const jlong size, const jboolean huge, const jboolean lock) {
	// If no huge memory pages should be employed, then use the simple C memory allocation function
	if (!huge) {
		free((void *) address);
		return JNI_TRUE;
	}

#ifdef __linux__
	// Deallocate the memory region
	if (munmap((void *) address, size) != 0) {
		perror("Error munmap-ing the hugepage file");
		fflush(stderr);
		return JNI_FALSE;
	}
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1mmap(JNIEnv *env, const jclass klass, const jobject jfd, const jlong size, const jboolean huge, const jboolean lock) {
#ifdef __linux__
	// Cache the fields of the class and get the file descriptor
	if (cacheFields(env) == JNI_FALSE) return 0;
	const int fd = (*env)->GetIntField(env, jfd, gCachedFields.descriptorField);

	// Map the file
	int flags = MAP_SHARED;
	if (huge) flags |= MAP_HUGETLB;
	if (lock) flags |= MAP_NORESERVE | MAP_LOCKED;
	void *map = mmap(NULL, size, PROT_READ | PROT_WRITE, flags, fd, 0);
	if (map == MAP_FAILED) {
		perror("Error memory mapping file");
		fflush(stderr);
		printf(" * File descriptor: %d\n", fd);
		printf(" * Size: %d (mod page = %d; mod huge = %d)\n",
				size,
				size % sysconf(_SC_PAGESIZE),
				size % Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1hugepage_1size(env, klass));
		printf(" * Huge: %s\n", huge ? "true" : "false");
		printf(" * Lock: %s\n", lock ? "true" : "false");
		fflush(stdout);
		return 0;
	}

	// Lock the file
	if (lock && mlock(map, size) != 0) {
		perror("Error locking the mapped memory");
		fflush(stderr);
	}

	// Return the memory mapping
	return (jlong) map;
#else
	return 0;
#endif
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1munmap(JNIEnv *env, const jclass klass, const jlong address, const jlong size, const jboolean huge, const jboolean lock) {
#ifdef __linux__
	if (munmap((void *) address, (size_t) size) != 0) {
		perror("Error unmapping file");
		fflush(stderr);
	}
#endif
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jbyte *) address);
}

JNIEXPORT jbyte JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	__asm__ volatile ("" : : : "memory");
	return *((volatile jbyte *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	*((jbyte *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1byte_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jbyte value) {
	__asm__ volatile ("" : : : "memory");
	*((volatile jbyte *) address) = value;
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jshort *) address);
}

JNIEXPORT jshort JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	__asm__ volatile ("" : : : "memory");
	return *((volatile jshort *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short(const JNIEnv *env, const jclass klass, const jlong address, jshort value) {
	*((jshort *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1short_1volatile(const JNIEnv *env, const jclass klass, const jlong address, jshort value) {
	__asm__ volatile ("" : : : "memory");
	*((volatile jshort *) address) = value;
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jint *) address);
}

JNIEXPORT jint JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	__asm__ volatile ("" : : : "memory");
	return *((volatile jint *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	*((jint *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1int_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jint value) {
	__asm__ volatile ("" : : : "memory");
	*((volatile jint *) address) = value;
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long(const JNIEnv *env, const jclass klass, const jlong address) {
	return *((jlong *) address);
}

JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address) {
	__asm__ volatile ("" : : : "memory");
	return *((volatile jlong *) address);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	*((jlong *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put_1long_1volatile(const JNIEnv *env, const jclass klass, const jlong address, const jlong value) {
	__asm__ volatile ("" : : : "memory");
	*((volatile jlong *) address) = value;
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1get(JNIEnv *env, const jclass klass, const jlong src, const jint size, const jbyteArray dest, const jint offset) {
	jbyte *destptr = (*env)->GetByteArrayElements(env, dest, NULL);
	memcpy((void *) (destptr + offset), (void *) src, size);
	(*env)->ReleaseByteArrayElements(env, dest, destptr, 0);
}

JNIEXPORT void JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1put(JNIEnv *env, const jclass klass, const jlong dest, const jint size, const jbyteArray src, const jint offset) {
	jbyte *srcptr = (*env)->GetByteArrayElements(env, src, NULL);
	memcpy((void *) dest, (void *) (srcptr + offset), size);
	(*env)->ReleaseByteArrayElements(env, src, srcptr, JNI_ABORT);
}


JNIEXPORT jlong JNICALL
Java_de_tum_in_net_ixy_memory_JniMemoryManager_c_1virt2phys(const JNIEnv *env, const jclass klass, const jlong address) {
#ifdef __linux__
	// Open the pagemap file
	const int fd = open("/proc/self/pagemap", O_RDONLY);
	if(fd == -1) {
		perror("Could not open /proc/self/pagemap file");
		fflush(stderr);
		return 0;
	}

	// Get the page size to compute the page number and offset
	const uint64_t pagesize = sysconf(_SC_PAGESIZE);
	const uint64_t page = (uint64_t) address / pagesize;

	// Move to the correct position
	if (lseek(fd, page * sizeof(void *), SEEK_SET) == -1) {
		perror("Could not move the cursor to the correct position");
		fflush(stderr);
		return 0;
	}

	// Read the physical address
	uint64_t phy = 0;
	if (read(fd, &phy, sizeof(void *)) == -1) {
		perror("Could not read the physical address");
		fflush(stderr);
		return 0;
	}

	// Close the pagemap file
	if (close(fd) != 0) {
		perror("Error closing the /proc/self/pagemap file");
		fflush(stderr);
	}

	// Return the physical address with the fixed offset
	const uint64_t offset = address % pagesize;
//	printf("Computing translation: %p\n", (void *) address);
//	printf(" * Offset = %x\n", (unsigned long long) offset);
//	printf(" * Page = %x\n", (unsigned long long) page);
//	printf(" * Physical = %d\n", (unsigned long long) phy);
//	printf(" * Phys. addr. = %p\n", (void *) (phy & 0x7fffffffffffff));
//	fflush(stdout);
	return (jlong) ((phy & 0x7fffffffffffffULL)*pagesize + offset);
#else
	return 0;
#endif
}

#ifdef __cplusplus
}
#endif
