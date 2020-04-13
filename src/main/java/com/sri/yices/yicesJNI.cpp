#include <jni.h>
#include <assert.h>
#include <gmp.h>
#include <yices.h>
#include <stdio.h>

#include <new>
#include <limits>

#include "com_sri_yices_Yices.h"

#if __YICES_VERSION > 2 || \
    (__YICES_VERSION == 2 && (__YICES_VERSION_MAJOR > 6 || \
                       (__YICES_VERSION_MAJOR == 6 && \
                        __YICES_VERSION_PATCHLEVEL > 1)))  
#define YICES_AT_LEAST_2_6_2 
#endif

#define YICES_ERROR_REQUIRES_AT_LEAST_2_6_2  -262


/*
 * On Windows, jint and int32_t are not the same type:
 * int32_t is the same int
 * jint    is the same as (long int)
 *
 * Even though (long int) is 32bit on windows, the compilers
 * complain when the mix pointers to jint an pointers to int32_t,
 * term_t, or type_t.
 *
 * To stop these warnings, we put explicit coercions in
 * calls to GetIntArrayElements and ReleaseIntArrayElements.
 * For example:
 *
 *    int32_t *aux = (int32_t *) env->GetIntArrayElements(a, copy);
 *    ...
 *    env->ReleaseIntArrayElements(a, (jint*) aux, JNI_ABORT);
 *
 */

static inline int32_t *array2int32(JNIEnv *env, jintArray a, jboolean *copy){
#ifdef MINGW
  return reinterpret_cast<int32_t *>(env->GetIntArrayElements(a, copy));
#else
  return env->GetIntArrayElements(a, copy);
#endif
}

static inline term_t *array2terms(JNIEnv *env, jintArray a, jboolean *copy){
  return array2int32(env, a, copy);
}

static inline type_t *array2types(JNIEnv *env, jintArray a, jboolean *copy){
  return array2int32(env, a, copy);
}

static inline void release_int32_elems(JNIEnv *env, jintArray a, int32_t *ptr, jint mode){
#ifdef MINGW
  env->ReleaseIntArrayElements(a, reinterpret_cast<jint*>(ptr), mode);
#else
  env->ReleaseIntArrayElements(a, ptr, mode);
#endif
}

static inline void release_int32_elems(JNIEnv *env, jintArray a, int32_t *ptr){
  release_int32_elems(env, a, ptr, JNI_ABORT);
}

static inline void release_term_elems(JNIEnv *env, jintArray a, term_t *ptr){
  release_int32_elems(env, a, ptr, JNI_ABORT);
}

static inline void release_term_elems(JNIEnv *env, jintArray a, term_t *ptr, jint mode){
  release_int32_elems(env, a, ptr, mode);
}

static inline void release_type_elems(JNIEnv *env, jintArray a, type_t *ptr){
  release_int32_elems(env, a, ptr, JNI_ABORT);
}

static inline void array2int_region(JNIEnv *env, jintArray a, jsize start, jsize end, int32_t *ptr){
#ifdef MINGW
  env->GetIntArrayRegions(a, start, end, reinterpret_cast<jint*>(ptr));
#else
  env->GetIntArrayRegion(a, start, end, ptr);
#endif
}

static inline void set_int_region(JNIEnv *env, jintArray a, jsize start, jsize end, const int32_t *ptr){
#ifdef MINGW
  env->SetIntArrayRegions(a, start, end, reinterpret_cast<jint*>(ptr));
#else
  env->SetIntArrayRegion(a, start, end, ptr);
#endif
}


/*
 * Out-of-memory handler: throws a C++ exception
 * that we can catch and convert to a Java exception.
 *
 * The default behavior of yices is to call "exit(1)" when
 * it runs out-of-memory, which woulld kill the JVM.
 */
static void throw_out_of_mem_exception() {
  throw std::bad_alloc();
}


/*
 * Code that throws the Java exception
 */
static void out_of_mem_exception(JNIEnv *env) {
  jclass e;
  jint code;

  code = 0;
  e = env->FindClass("com/sri/yices/OutOfMemory");
  if (e == NULL) e = env->FindClass("java/lang/OutOfMemoryError");

  if (e != NULL) {
    code = env->ThrowNew(e, NULL);
  }
  if (e == NULL || code < 0) {
    // Something went  badly wrong.
    // We check whether an exception is pending. If not we report a fatal error
    if (! env->ExceptionCheck()) {
      env->FatalError("Out-of-memory in Yices JNI.\nFailed to throw an exception\n");
    }
  }
}


/*
 * Convert array a of size n into a Java Int array
 * - return NULL if we can't allocate the new array and throw an exception
 */
static jintArray convertToIntArray(JNIEnv *env, int32_t n, const int32_t *a) {
  jintArray b;

  b = env->NewIntArray(n);
  if (b == NULL) {
    out_of_mem_exception(env);
  } else {
    set_int_region(env, b, 0, n, a);
  }
  return b;
}


/*
 * Convert a string (s may be NULL);
 */
static jstring convertToString(JNIEnv *env, const char *s) {
  jstring b = NULL;

  if (s != NULL) {
    b = env->NewStringUTF(s);
    if (b == NULL) {
      out_of_mem_exception(env);
    }
  }
  return b;
}


/*
 * Convert to an array of Booleans:
 * - the input is an array a of integers
 * - a[i] is converted to false if a[i] = 0 or to true if a[i] != 0
 */
static jbooleanArray convertToBoolArray(JNIEnv *env, int32_t n, const int32_t *a) {
  jbooleanArray b;

  b = env->NewBooleanArray(n);
  if (b == NULL) {
    out_of_mem_exception(env);
  } else {
    jboolean *aux = env->GetBooleanArrayElements(b, NULL);
    if (aux == NULL) {
      out_of_mem_exception(env);
    } else {
      for (int32_t i = 0; i<n; i++) {
        aux[i] = (a[i] != 0);
      }
      env->ReleaseBooleanArrayElements(b, aux, 0); // copy back
    }
  }

  return b;
}

/*
 * Clone array a: make a copy
 * - return NULL and throw an exception if we can't clone
 * - the JVM may make a copy already. In this case, we just store "true" in *copy
 * - otherwise, we allocate an auxiliary array, copy a into it, and store "false" in *copy
 */
static int32_t *cloneIntArray(JNIEnv *env, jintArray a, jboolean *copy) {
  int32_t *aux = NULL;

  aux = array2int32(env, a, copy);
  if (aux == NULL) {
    out_of_mem_exception(env);
  } else if (!copy) {
    jsize n = env->GetArrayLength(a);
    int32_t *b = aux;
    try {
      aux = new int32_t[n];
      for (int32_t i=0; i<n; i++) {
        aux[i] = b[i];
      }
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
      aux = NULL;
    }
    release_int32_elems(env, a, b);
  }

  return aux;
}

/*
 * Free memory used by the previous function if any
 */
static void freeClonedIntArray(JNIEnv *env, jintArray a, int32_t *aux, jboolean *copy) {
  if (copy) {
    release_int32_elems(env, a, aux);
  } else {
    delete [] aux;
  }
}


/*
 * CONVERSION TO/FROM GMP NUMBERS
 */

/*
 * To support conversion of mpz/mpq to BigIntegers or Rationals,
 * we construct byte arrays in the format used by Java BigIntegers.
 *
 * A big number is represented as an array of n bytes big-endian:
 *     b[0] = most significant byte
 *     ...
 *   b[n-1] = least siginficant byte
 *
 * The number is negative if b[0] is negative.
 *
 * Examples:
 *   N =  127 --> n = 1, b[0] =  127
 *   N = -127 --> n = 1, b[0] = -127
 *   N =  128 --> n = 2, b[0] =  0, b[1] = -128
 *   N = -128 --> n = 1, b[0] = -128
 *   N =  255 --> n = 2, b[0] =  0, b[1] = -1
 *   N = -255 --> n = 2, b[0] = -1, b[1] = 1
 *   N =  256 --> n = 2, b[0] =  1, b[1] = 0
 *   N = -256 --> n = 2, b[0] = -1, b[1] = 0
 */

// 2s-complement negation of b[0 ... n-1]
static void negate_bytes(jbyte *b, size_t n) {
  unsigned c = 1;
  unsigned k = n;

  do {
    k--;
    unsigned x = (((uint8_t) b[k]) ^ 255) + c;
    b[k] = x & 255;
    c = x >> 8;
  } while (k > 0);
}

/*
 * Convert z to an array of bytes
 * - GMP uses a magnitude + sign representation
 * - we first call mpz_export to convert the magnitude to a byte array,
 *   then we negate this array if z is negative
 * - we allocate one more byte than (ceil(numbits(z)/8)) because we
 *   may need an extra byte to store the sign bit.
 *
 * Returns NULL and raise an exception if allocation fails.
 */
static jbyteArray mpz_to_byte_array(JNIEnv *env, mpz_t z) {
  jbyte buffer[32];
  jbyte *aux;
  jbyteArray result = NULL;
  size_t nbits = mpz_sizeinbase(z, 2);
  size_t nbytes = ((nbits + 7) >> 3) + 1;

  // nbits is positive (even if z == 0)
  assert(nbytes >= 2);

#if 0
  fprintf(stderr, "mpz_to_byte_array\n");
  fprintf(stderr, "input z = ");
  mpz_out_str(stderr, 10, z);
  fprintf(stderr, "\n");
  fprintf(stderr, "nbits = %d, nbytes = %d\n", (int) nbits, (int) nbytes);
#endif

  try {
    if (nbytes <= 32) {
      aux = buffer;
    } else if (nbytes <= static_cast<size_t>(std::numeric_limits<int32_t>::max())) {
      aux = new jbyte[nbytes];
    } else {
      // give up now: the size is too large
      throw std::bad_alloc();
    }

    // if z = 0, mpz_export will write nothing in aux[1]
    // in all other cases, mpz_export will store something in aux[1]
    // so we initialize aux[1] to 0.
    aux[0] = 0;
    aux[1] = 0;
    mpz_export(aux + 1, NULL, 1, 1, 0, 0, z);

#if 0
    fprintf(stderr, "after mpz_export:\n");
    for (size_t i=0; i<nbytes; i++) fprintf(stderr, " %d", aux[i]);
    fprintf(stderr, "\n");
#endif

    if (mpz_sgn(z) < 0) {
      negate_bytes(aux, nbytes);
#if 0
      fprintf(stderr, "after negate:\n");
      for (size_t i=0; i<nbytes; i++) fprintf(stderr, " %d", aux[i]);
      fprintf(stderr, "\n");
#endif
    }

    // aux[0] is redundant if it's 0 and aux[1] is positive
    // or if it's -1 and aux[1] is negative
    bool r = (aux[0] == 0 && aux[1] >= 0) || (aux[0] == -1 && aux[1] < 0);

#if 0
    fprintf(stderr, "r = %d\n", r);
#endif

    result = env->NewByteArray(nbytes - r);
    if (result == NULL) {
      out_of_mem_exception(env);
    } else {
      env->SetByteArrayRegion(result, 0, nbytes - r, aux + r);
    }

    if (nbytes > 32) {
      delete[] aux;
    }

  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}


/*
 * Inverse operation: convert a byte array to mpz
 * - n = array size, b = array of bytes
 * - return true if this works, false if we can't allocate memory
 */
static void byte_array_to_mpz(mpz_t z, jbyte *b, jsize n) {
  if (b[0] < 0) {
    negate_bytes(b, n);
    mpz_import(z, n, 1, 1, 0, 0, b);
    mpz_neg(z, z);
  } else {
    mpz_import(z, n, 1, 1, 0, 0, b);
  }
}


/*
 * For testing
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_testMpzToBytes(JNIEnv *env, jclass, jstring s) {
  jbyteArray result = NULL;
  mpz_t z;
  const char *aux = env->GetStringUTFChars(s, NULL);

  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    mpz_init(z);
    mpz_set_str(z, aux, 0);
    result = mpz_to_byte_array(env, z);
    mpz_clear(z);
  }

  return result;
}


JNIEXPORT void JNICALL Java_com_sri_yices_Yices_testBytesToMpz(JNIEnv *env, jclass, jbyteArray a) {
  jsize n;
  jbyte *b;

  n = env->GetArrayLength(a);
  b = env->GetByteArrayElements(a, NULL);
  if (b == NULL) {
    out_of_mem_exception(env);
  } else {
    mpz_t z;

    mpz_init(z);
    byte_array_to_mpz(z, b, n);
    fprintf(stdout, "Got mpz number: ");
    mpz_out_str(stdout, 10, z);
    fprintf(stdout, "\n");
    mpz_clear(z);

    env->ReleaseByteArrayElements(a, b, JNI_ABORT); // don't change a
  }
}


/*
 * Convert the given array of bytes to an integer constant
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bytesToIntConstant(JNIEnv *env, jclass, jbyteArray a) {
  jint result = -1;
  jsize n;
  jbyte *b;

  n = env->GetArrayLength(a);
  b = env->GetByteArrayElements(a, NULL);
  if (b == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      mpz_t z;

      mpz_init(z);
      byte_array_to_mpz(z, b, n);
      result = yices_mpz(z);
      mpz_clear(z);

    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }

    env->ReleaseByteArrayElements(a, b, JNI_ABORT); // don't change a
  }

  return result;
}

/*
 * Convert arrays of bytes num/den to a rational constant
 * - return -1 if the denominator is zero
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bytesToRationalConstant(JNIEnv *env, jclass, jbyteArray num, jbyteArray den) {
  jint result = -1;
  jsize num_size, den_size;
  jbyte *num_bytes, *den_bytes;

  num_size = env->GetArrayLength(num);
  num_bytes = env->GetByteArrayElements(num, NULL);
  den_size = env->GetArrayLength(den);
  den_bytes = env->GetByteArrayElements(den, NULL);
  if (num_bytes == NULL || den_bytes == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      mpq_t q;

      mpq_init(q);
      byte_array_to_mpz(mpq_numref(q), num_bytes, num_size);
      byte_array_to_mpz(mpq_denref(q), den_bytes, den_size);
      if (mpz_sgn(mpq_denref(q)) != 0) {
        // the denominator is non-zero
        mpq_canonicalize(q);
        result = yices_mpq(q);
      }
      mpq_clear(q);

    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }

  if (num_bytes != NULL) env->ReleaseByteArrayElements(num, num_bytes, JNI_ABORT);
  if (den_bytes != NULL) env->ReleaseByteArrayElements(den, den_bytes, JNI_ABORT);

  return result;
}




#if 0
/*
 * Convert a Java BigInteger object x to mpz_t
 */
static void convertBigIntToMpz(JNIEnv *env, jobject x, mpz_t z) {
  jclass bigIntClass = env->FindClass("java/math/BigInteger");
  assert(bigIntClass != NULL);

  jmethodID signum =  env->GetMethodID(bigIntClass, "signum", "()I");
  jmethodID toByteArray = env->GetMethodID(bigIntClass, "toByteArray", "()[B");
  assert(signum != NULL && toByteArray != NULL);

  jint sign = env->CallIntMethod(x, signum);
  jbyteArray bytes = reinterpret_cast<jbyteArray>(env->CallObjectMethod(x, toByteArray));
  jsize n = env->GetArrayLength(bytes);
  jbyte *b = env->GetByteArrayElements(bytes, NULL);

  fprintf(stderr, "sign = %d\n", sign);
  fprintf(stderr, "number of bytes = %d\n", env->GetArrayLength(bytes));
  if (b != NULL) {
    for (jsize i=0; i<n; i++) {
      fprintf(stderr, "byte[%d] = %u (%d)\n", i, (unsigned char) b[i], b[i]);
    }
    env->ReleaseByteArrayElements(bytes, b, JNI_ABORT);
  }
  fprintf(stderr, "\n");
}

#endif


/*
 * COERCIONS AND CHECKS FOR ARRAYS
 */

/*
 * Check whether all elements of array a are positive or zero
 * - n = array size
 */
static bool all_positive_longs(jsize n, jlong *a) {
  for (jsize i = 0; i<n; i++) {
    if (a[i] < 0) return false;
  }
  return true;
}

#if 0
// NOT USED ANYMORE
/*
 * Cast from jlong* to int64_t*
 * - clang++ complains when I try a direct static_cast
 *   because jlong* is the same as  (long *)
 *       and int64_t* is the same as (long long *)
 *  (at least on my Mac).
 */
static inline int64_t *to_int64ptr(jlong *x) {
  assert(sizeof(int64_t) == sizeof(jlong));
  return static_cast<int64_t*>(static_cast<void *>(x));
}

/*
 * Variant: cast to uint64_t*
 */
static inline uint64_t *to_uint64ptr(jlong *x) {
  assert(sizeof(uint64_t) == sizeof(jlong));
  return static_cast<uint64_t*>(static_cast<void *>(x));
}

#endif

/* =================== API =================== */

/*
 * VERSION DATA
 */
JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_version(JNIEnv *env, jclass cls) {
  return convertToString(env, yices_version);
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_buildArch(JNIEnv *env, jclass cls) {
  return convertToString(env, yices_build_arch);
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_buildMode(JNIEnv *env, jclass cls) {
  return convertToString(env, yices_build_mode);
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_buildDate(JNIEnv *env, jclass cls) {
  return convertToString(env, yices_build_date);
}

/*
 * NOTE: the JNI spec JNI_FALSE = 0 and JNI_TRUE = 1
 * yices_has_mcsat returns 0 for false and 1 for true so we're good.
 */
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_hasMcsat(JNIEnv *env, jclass cls) {
  int32_t x = yices_has_mcsat();
  assert(0 == x || 1 == x);
  return (jboolean) x;
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_isThreadSafe(JNIEnv *env, jclass cls) {
  int32_t x = yices_is_thread_safe();
  assert(0 == x || 1 == x);
  return (jboolean) x;
}



/*
 * GLOBAL INITIALIZATION/EXIT/RESET
 */
JNIEXPORT void JNICALL Java_com_sri_yices_Yices_init(JNIEnv *, jclass) {
  yices_init();
  yices_set_out_of_mem_callback(throw_out_of_mem_exception);
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_exit(JNIEnv *, jclass) {
  yices_exit();
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_reset(JNIEnv *, jclass) {
  yices_reset();
}



/*
 * Error reports
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_errorCode(JNIEnv *, jclass) {
  return yices_error_code();
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_errorString(JNIEnv *env, jclass) {
  jstring result;
  char *e;

  e = yices_error_string();
  result = convertToString(env, e);
  yices_free_string(e);
  return result;
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_resetError(JNIEnv *, jclass) {
  yices_clear_error();
}


// to test the throw exception code
JNIEXPORT void JNICALL Java_com_sri_yices_Yices_testException(JNIEnv *env, jclass) {
  out_of_mem_exception(env);
}

/*
 * Type constructors: these types are predefined and can't cause out-of-memory
 * exceptions.
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_boolType(JNIEnv *, jclass) {
  return yices_bool_type();
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_realType(JNIEnv *, jclass) {
  return yices_real_type();
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_intType(JNIEnv *, jclass) {
  return yices_int_type();
}

/*
 * n is the number of bits.
 * If n<0, we replace it by zero. Yices will report an error if n is 0.
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvType(JNIEnv *env, jclass, jint n) {
  uint32_t nb = n<0 ? 0 : n;
  try {
    return yices_bv_type(nb);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

/*
 * Scalar type: c = cardinality: it must be positive. As above, we convert c<0 to 0.
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_newScalarType(JNIEnv *env, jclass, jint c) {
  uint32_t card = c<0 ? 0 : c;
  try {
    return yices_new_scalar_type(card);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_newUninterpretedType(JNIEnv *env, jclass) {
  try {
    return yices_new_uninterpreted_type();
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}


JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_tupleType(JNIEnv *env, jclass, jintArray a) {
  jsize n = env->GetArrayLength(a);
  if (n == 0) {
    // force an error in Yices
    return yices_tuple_type(0, NULL);
  }
  assert(n > 0);
  type_t *tau = array2types(env, a, NULL);
  if (tau == NULL) {
    out_of_mem_exception(env);
    return -1;
  }
  jint result = -1;
  try {
    result = yices_tuple_type(n, tau);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  release_type_elems(env, a, tau);
  return result;
}


JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_functionType(JNIEnv *env, jclass, jint range, jintArray domain) {
  jsize n = env->GetArrayLength(domain);
  if (n == 0) {
    // force an error
    return yices_function_type(0, NULL, range);
  }
  assert(n > 0);
  type_t *tau = array2types(env, domain, NULL);
  if (tau == NULL) {
    out_of_mem_exception(env);
    return -1;
  }
  jint result = -1;
  try {
    result = yices_function_type(n, tau, range);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  release_type_elems(env, domain, tau);
  return result;
}


/*
 * CHECK/EXPLORE TYPES
 */
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsBool(JNIEnv *, jclass, jint tau) {
  return yices_type_is_bool(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsInt(JNIEnv *, jclass, jint tau) {
  return yices_type_is_int(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsReal(JNIEnv *, jclass, jint tau) {
  return yices_type_is_real(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsArithmetic(JNIEnv *, jclass, jint tau) {
  return yices_type_is_arithmetic(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsBitvector(JNIEnv *, jclass, jint tau) {
  return yices_type_is_bitvector(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsScalar(JNIEnv *, jclass, jint tau) {
  return yices_type_is_scalar(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsUninterpreted(JNIEnv *, jclass, jint tau) {
  return yices_type_is_uninterpreted(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsTuple(JNIEnv *, jclass, jint tau) {
  return yices_type_is_tuple(tau);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_typeIsFunction(JNIEnv *, jclass, jint tau) {
  return yices_type_is_function(tau);
}

/*
 * Check whether sigma is a subtype of tau:
 * - this may allocate memory so we check for out-of-memory error here
 */
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_isSubtype(JNIEnv *env, jclass, jint tau, jint sigma) {
  try {
    return yices_test_subtype(tau, sigma);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return JNI_FALSE;
  }
}

/*
 * Check whether sigma and tau are compatible
 * - this may allocate memory so we check for out-of-memory error here
 */
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_areCompatible(JNIEnv *env, jclass, jint tau, jint sigma) {
  try {
    return yices_compatible_types(tau, sigma);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return JNI_FALSE;
  }
}

/*
 * Number of bits in a bitvector type
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvTypeSize(JNIEnv *, jclass, jint tau) {
  return yices_bvtype_size(tau);
}

/*
 * Cardinality of a scalar type
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_scalarTypeCard(JNIEnv *, jclass, jint tau) {
  return yices_scalar_type_card(tau);
}

/*
 * Number of children of type tau
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_typeNumChildren(JNIEnv *, jclass, jint tau) {
  return yices_type_num_children(tau);
}

/*
 * Get i-th child of type tau
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_typeChild(JNIEnv *, jclass, jint tau, jint i) {
  return yices_type_child(tau, i);
}

/*
 * TYPE NAMES
 */

/*
 * Collect all the children of type tau
 * return NULL is tau is not a valid type
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_typeChildren(JNIEnv *env, jclass, jint tau) {
  type_vector_t aux;
  jintArray result = NULL;
  int32_t code;

  try {
    yices_init_type_vector(&aux);

    code = yices_type_children(tau, &aux);
    if (code >= 0) {
      result = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_type_vector(&aux);

  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}


/*
 * Give a a name to type tau
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_setTypeName(JNIEnv *env, jclass, jint tau, jstring name) {
  jint code = -1;
  const char *s = env->GetStringUTFChars(name, NULL);

  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      code = yices_set_type_name(tau, s);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(name, s);
  }

  return code;
}


JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_getTypeName(JNIEnv *env, jclass, jint tau) {
  return convertToString(env, yices_get_type_name(tau));
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_getTypeByName(JNIEnv *env, jclass, jstring name) {
  jint tau = -1;
  const char *s = env->GetStringUTFChars(name, NULL);

  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      tau = yices_get_type_by_name(s);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(name, s);
  }

  return tau;
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_removeTypeName(JNIEnv *env, jclass, jstring name) {
  const char *s = env->GetStringUTFChars(name, NULL);

  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    yices_remove_type_name(s);
    env->ReleaseStringUTFChars(name, s);
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_clearTypeName(JNIEnv *env, jclass, jint tau) {
  return yices_clear_type_name(tau);
}


/*
 * Convert tau to a string by calling the Yices pretty printer
 * We print into an array of 80 columns x 4 lines
 */
JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_typeToString(JNIEnv *env, jclass, jint tau) {
  char *s;
  jstring result;

  try {
    s = yices_type_to_string(tau, 80, 4, 0);
    result = convertToString(env, s);
    yices_free_string(s); // this is safe even if s is NULL
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}

/*
 * Parse s as type using the Yices syntax
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_parseType(JNIEnv *env, jclass, jstring s) {
  jint result = -1;
  const char *aux = env->GetStringUTFChars(s, NULL);

  if (aux == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_parse_type(aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(s, aux);
  }

  return result;
}


/*
 * GENERIC TERM CONSTRUCTORS
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_mkTrue(JNIEnv *, jclass) {
  return yices_true(); // Can't cause out-of-mem error
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_mkFalse(JNIEnv *, jclass) {
  return yices_false();
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_mkConstant(JNIEnv *env, jclass, jint tau, jint idx) {
  try {
    return yices_constant(tau, idx);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_newUninterpretedTerm(JNIEnv *env, jclass, jint tau) {
  try {
    return yices_new_uninterpreted_term(tau);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_newVariable(JNIEnv *env, jclass, jint tau) {
  try {
    return yices_new_variable(tau);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

// function application: f = function, arg = arguments
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_funApplication(JNIEnv *env, jclass, jint f, jintArray arg) {
  jsize n = env->GetArrayLength(arg);
  term_t *a = array2terms(env, arg, NULL);
  jint result = -1;

  if (a == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_application(f, n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseIntArrayElements(arg, a, JNI_ABORT);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_ifThenElse(JNIEnv *env, jclass, jint cond, jint iftrue, jint iffalse) {
  try {
    return yices_ite(cond, iftrue, iffalse);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_eq(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_eq(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_neq(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_neq(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_not(JNIEnv *, jclass, jint arg) {
  return yices_not(arg); // can't cause out-of-mem
}


/*
 * For and/or/xor, we make a copy of the arg array (otherwise the array may be modified).
 */
#define AUX_SIZE 10

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_and(JNIEnv *env, jclass, jintArray arg) {
  int32_t aux[AUX_SIZE]; // avoid alloc if arg is small
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n <= AUX_SIZE) {
    array2int_region(env, arg, 0, n, aux);
    try {
      result = yices_and(n, aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  } else {
    jboolean copy;
    int32_t *a = cloneIntArray(env, arg, &copy);
    try {
      result = yices_and(n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    freeClonedIntArray(env, arg, a, &copy);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_or(JNIEnv *env, jclass, jintArray arg) {
  int32_t aux[AUX_SIZE]; // avoid malloc if we can
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n < AUX_SIZE) {
    array2int_region(env, arg, 0, n, aux);
    try {
      result = yices_or(n, aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  } else {
    jboolean copy;
    int32_t *a = cloneIntArray(env, arg, &copy);
    try {
      result = yices_or(n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    freeClonedIntArray(env, arg, a, &copy);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_xor(JNIEnv *env, jclass, jintArray arg) {
  int32_t aux[AUX_SIZE]; // avoid malloc if we can
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n < AUX_SIZE) {
    array2int_region(env, arg, 0, n, aux);
    try {
      result = yices_xor(n, aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  } else {
    jboolean copy;
    int32_t *a = cloneIntArray(env, arg, &copy);
    try {
      result = yices_xor(n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    freeClonedIntArray(env, arg, a, &copy);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_iff(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_iff(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_implies(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_implies(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_tuple(JNIEnv *env, jclass, jintArray arg) {
  jsize n = env->GetArrayLength(arg);
  term_t *a = array2terms(env, arg, NULL);
  jint result = -1;

  if (a == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_tuple(n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    release_term_elems(env, arg, a);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_select(JNIEnv *env, jclass, jint idx, jint tuple) {
  try {
    return yices_select(idx, tuple);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_tupleUpdate(JNIEnv *env, jclass, jint tuple, jint idx, jint newval) {
  try {
    return yices_tuple_update(tuple, idx, newval);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_functionUpdate(JNIEnv *env, jclass, jint fun, jintArray arg, jint newval) {
  jsize n = env->GetArrayLength(arg);
  term_t *a = array2terms(env, arg, NULL);
  jint result = -1;

  if (a == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_update(fun, n, a, newval);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    release_term_elems(env, arg, a);
  }
  return result;
}

// common variant: corresponding to array update (i.e., fun has arity one)
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_functionUpdate1(JNIEnv *env, jclass, jint fun, jint arg, jint newval) {
  try {
    return yices_update1(fun, arg, newval);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

// yices_distinct may modify its argument so we make a copy of arg here
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_distinct(JNIEnv *env, jclass, jintArray arg) {
  int32_t aux[AUX_SIZE]; // avoid malloc if we can
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n < AUX_SIZE) {
    array2int_region(env, arg, 0, n, aux);
    try {
      result = yices_distinct(n, aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  } else {
    jboolean copy;
    int32_t *a = cloneIntArray(env, arg, &copy);
    try {
      result = yices_distinct(n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    freeClonedIntArray(env, arg, a, &copy);
  }

  return result;
}


JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_forall(JNIEnv *env, jclass, jintArray var, jint body) {
  int32_t aux[AUX_SIZE]; // avoid malloc if we can
  jint result = -1;
  jsize n = env->GetArrayLength(var);

  if (n < AUX_SIZE) {
    array2int_region(env, var, 0, n, aux);
    try {
      result = yices_forall(n, aux, body);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  } else {
    jboolean copy;
    int32_t *a = cloneIntArray(env, var, &copy);
    try {
      result = yices_forall(n, a, body);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    freeClonedIntArray(env, var, a, &copy);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_exists(JNIEnv *env, jclass, jintArray var, jint body) {
  int32_t aux[AUX_SIZE]; // avoid malloc if we can
  jint result = -1;
  jsize n = env->GetArrayLength(var);

  if (n < AUX_SIZE) {
    array2int_region(env, var, 0, n, aux);
    try {
      result = yices_exists(n, aux, body);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  } else {
    jboolean copy;
    int32_t *a = cloneIntArray(env, var, &copy);
    try {
      result = yices_exists(n, a, body);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    freeClonedIntArray(env, var, a, &copy);
  }

  return result;
}


JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_lambda(JNIEnv *env, jclass, jintArray var, jint body) {
  jsize n = env->GetArrayLength(var);
  term_t *a = array2terms(env, var, NULL);
  jint result = -1;

  if (a == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_lambda(n, a, body);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    release_term_elems(env, var, a);
  }
  return result;
}


/*
 * ARITHMETIC TERMS
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_zero(JNIEnv *env, jclass) {
  try {
    return yices_zero();
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }

}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_mkIntConstant(JNIEnv *env, jclass, jlong x) {
  try {
    return yices_int64(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_mkRationalConstant(JNIEnv *env, jclass, jlong num, jlong den) {
  /*
   * Yices wants the denominator to be non-negative
   * We could try to negate both num and den is den < 0,
   * but there are risks of numerical overflows if we try that.
   *
   * It's safer to report an error here.
   */
  if (den < 0) {
    return -1;
  }
  try {
    return yices_rational64(num, den);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}


JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_parseRational(JNIEnv *env, jclass, jstring s) {
  jint result = -1;
  const char *aux = env->GetStringUTFChars(s, NULL);

  if (aux == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_parse_rational(aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(s, aux);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_parseFloat(JNIEnv *env, jclass, jstring s) {
  jint result = -1;
  const char *aux = env->GetStringUTFChars(s, NULL);

  if (aux == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_parse_float(aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(s, aux);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_add__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_add(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_sub(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_sub(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_neg(JNIEnv *env, jclass, jint arg) {
  try {
    return yices_neg(arg);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_mul__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_mul(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_square(JNIEnv *env, jclass, jint arg) {
  try {
    return yices_square(arg);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_power(JNIEnv *env, jclass, jint arg, jint exponent) {
  if (exponent < 0) {
    return -1; // negative exponents are not supported
  }
  try {
    return yices_power(arg, exponent);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_add___3I(JNIEnv *env, jclass, jintArray arg) {
  jsize n = env->GetArrayLength(arg);
  term_t *a = array2terms(env, arg, NULL);
  jint result = -1;

  if (a == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_sum(n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    release_term_elems(env, arg, a);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_mul___3I(JNIEnv *env, jclass, jintArray arg) {
  jsize n = env->GetArrayLength(arg);
  term_t *a = array2terms(env, arg, NULL);
  jint result = -1;

  if (a == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_product(n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    release_term_elems(env, arg, a);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_div(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_division(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_idiv(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_idiv(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_imod(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_imod(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_abs(JNIEnv *env, jclass, jint x) {
  try {
    return yices_abs(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_floor(JNIEnv *env, jclass, jint x) {
  try {
    return yices_floor(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_ceil(JNIEnv *env, jclass, jint x) {
  try {
    return yices_ceil(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_intPoly(JNIEnv *env, jclass, jlongArray coeff, jintArray t) {
  jint result = -1;
  jsize n = env->GetArrayLength(coeff);

  if (n == env->GetArrayLength(t)) {
    // the two arrays must have the same size
    /*
     * TODO: we could filter out the case n=0
     * or use AUX_SIZE'd arrays for n small?
     */
    int32_t *a = array2int32(env, t, NULL);
    jlong *c = env->GetLongArrayElements(coeff, NULL);
    if (a == NULL || c == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_poly_int64(n, reinterpret_cast<int64_t*>(c), a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
    }
    if (c != NULL) env->ReleaseLongArrayElements(coeff, c, JNI_ABORT);
    if (a != NULL) release_int32_elems(env, t, a);
  }

  return result;
}


JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_rationalPoly(JNIEnv *env, jclass, jlongArray num, jlongArray den, jintArray t) {
  jint result = -1;
  jsize n = env->GetArrayLength(num);

  if (n == env->GetArrayLength(den) &&
      n == env->GetArrayLength(t)) {
    // the three arrays must have the same size
    /*
     * TODO: we could filter out the case n=0
     * or use AUX_SIZE'd arrays for n small?
     */
    int32_t *a = array2int32(env, t, NULL);
    jlong *p = env->GetLongArrayElements(num, NULL);
    jlong *q = env->GetLongArrayElements(den, NULL);
    if (a == NULL || p == NULL || q == NULL) {
      out_of_mem_exception(env);
    } else if (all_positive_longs(n, q)) {
      // fail if den[i] < 0 for some i
      try {
        result = yices_poly_rational64(n, reinterpret_cast<int64_t*>(p), reinterpret_cast<uint64_t*>(q), a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
    }
    if (q != NULL) env->ReleaseLongArrayElements(den, q, JNI_ABORT);
    if (p != NULL) env->ReleaseLongArrayElements(num, p, JNI_ABORT);
    if (a != NULL) release_int32_elems(env, t, a);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_divides(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_divides_atom(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_isInt(JNIEnv *env, jclass, jint x) {
  try {
    return yices_is_int_atom(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithEq(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_arith_eq_atom(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithNeq(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_arith_neq_atom(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithGeq(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_arith_geq_atom(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithLeq(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_arith_leq_atom(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithGt(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_arith_gt_atom(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithLt(JNIEnv *env, jclass, jint x, jint y) {
  try {
    return yices_arith_lt_atom(x, y);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithEq0(JNIEnv *env, jclass, jint x) {
  try {
    return yices_arith_eq0_atom(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithNeq0(JNIEnv *env, jclass, jint x) {
  try {
    return yices_arith_neq0_atom(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithGeq0(JNIEnv *env, jclass, jint x) {
  try {
    return yices_arith_geq0_atom(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithLeq0(JNIEnv *env, jclass, jint x) {
  try {
    return yices_arith_leq0_atom(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithGt0(JNIEnv *env, jclass, jint x) {
  try {
    return yices_arith_gt0_atom(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_arithLt0(JNIEnv *env, jclass, jint x) {
  try {
    return yices_arith_lt0_atom(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

/*
 * BITVECTOR TERMS
 */
// convert x to a bitvector od n bits
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvConst(JNIEnv *env, jclass, jint n, jlong x) {
  jint result = -1;

  if (n > 0) {
    try {
      result = yices_bvconst_int64(n, x);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvZero(JNIEnv *env, jclass, jint n) {
  jint result = -1;

  if (n > 0) {
    try {
      result = yices_bvconst_zero(n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvOne(JNIEnv *env, jclass, jint n) {
  jint result = -1;

  if (n > 0) {
    try {
      result = yices_bvconst_one(n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvMinusOne(JNIEnv *env, jclass, jint n) {
  jint result = -1;

  if (n > 0) {
    try {
      result = yices_bvconst_minus_one(n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvConstFromIntArray(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n >= 0) {
    int32_t *a = array2int32(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvconst_from_array(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_int32_elems(env, arg, a);
    }
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_parseBvBin(JNIEnv *env, jclass, jstring s) {
  jint result = -1;
  const char *aux = env->GetStringUTFChars(s, NULL);

  if (aux == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_parse_bvbin(aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(s, aux);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_parseBvHex(JNIEnv *env, jclass, jstring s) {
  jint result = -1;
  const char *aux = env->GetStringUTFChars(s, NULL);

  if (aux == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_parse_bvhex(aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(s, aux);
  }

  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvAdd__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvadd(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSub(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvsub(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvNeg(JNIEnv *env, jclass, jint arg) {
  try {
    return yices_bvneg(arg);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvMul__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvmul(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSquare(JNIEnv *env, jclass, jint arg) {
  try {
    return yices_bvsquare(arg);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvPower(JNIEnv *env, jclass, jint arg, jint exponent) {
  if (exponent < 0) {
    return -1; // negative exponents are not supported
  }
  try {
    return yices_bvpower(arg, exponent);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvDiv(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvdiv(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvRem(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvrem(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSDiv(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvsdiv(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSRem(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvsrem(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSMod(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvsmod(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvNot(JNIEnv *env, jclass, jint arg) {
  try {
    return yices_bvnot(arg);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvAnd__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvand2(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvOr__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvor2(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvXor__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvxor2(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvNand(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvnand(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvNor(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvnor(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvXNor(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvxnor(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvShl(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvshl(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvLshr(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvlshr(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvAshr(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvashr(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvAdd___3I(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n > 0) {
    term_t *a = array2terms(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvsum(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_term_elems(env, arg, a);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvMul___3I(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n > 0) {
    term_t *a = array2terms(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvproduct(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_term_elems(env, arg, a);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvAnd___3I(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n > 0) {
    term_t *a = array2terms(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvand(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_term_elems(env, arg, a);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvOr___3I(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n > 0) {
    term_t *a = array2terms(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvor(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_term_elems(env, arg, a);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvXor___3I(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n > 0) {
    term_t *a = array2terms(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvxor(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_term_elems(env, arg, a);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvShiftLeft0(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_shift_left0(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvShiftLeft1(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_shift_left1(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvShiftRight0(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_shift_right0(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvShiftRight1(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_shift_right1(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvAShiftRight(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_ashift_right(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvRotateLeft(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_rotate_left(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvRotateRight(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_rotate_right(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvExtract(JNIEnv *env, jclass, jint arg, jint i, jint j) {
  jint result = -1;

  if (i >= 0 && j >= 0) {
    try {
      result = yices_bvextract(arg, i, j);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvExtractBit(JNIEnv *env, jclass, jint arg, jint i) {
  jint result = -1;

  if (i >= 0) {
    try {
      result = yices_bitextract(arg, i);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvFromBoolArray(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n > 0) {
    term_t *a = array2terms(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvarray(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_term_elems(env, arg, a);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvConcat__II(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvconcat2(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvConcat___3I(JNIEnv *env, jclass, jintArray arg) {
  jint result = -1;
  jsize n = env->GetArrayLength(arg);

  if (n > 0) {
    term_t *a = array2terms(env, arg, NULL);
    if (a == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = yices_bvconcat(n, a);
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
      release_term_elems(env, arg, a);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvRepeat(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_bvrepeat(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSignExtend(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_sign_extend(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvZeroExtend(JNIEnv *env, jclass, jint arg, jint n) {
  jint result = -1;

  if (n >= 0) {
    try {
      result = yices_zero_extend(arg, n);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvRedAnd(JNIEnv *env, jclass, jint arg) {
  try {
    return yices_redand(arg);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvRedOr(JNIEnv *env, jclass, jint arg) {
  try {
    return yices_redor(arg);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvRedComp(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_redcomp(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvEq(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bveq_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvNeq(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvneq_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvGe(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvge_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvGt(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvgt_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvLe(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvle_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvLt(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvlt_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSGe(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvsge_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSGt(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvsgt_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSLe(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvsle_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_bvSLt(JNIEnv *env, jclass, jint left, jint right) {
  try {
    return yices_bvslt_atom(left, right);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return -1;
  }
}


/*
 * ACCESSORS AND CHECKS
 */

// These shouldn't caue out-of-memory exception
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_typeOfTerm(JNIEnv *env, jclass, jint x) {
  return yices_type_of_term(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsBool(JNIEnv *env, jclass, jint x) {
  return yices_term_is_bool(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsInt(JNIEnv *env, jclass, jint x) {
  return yices_term_is_int(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsReal(JNIEnv *env, jclass, jint x) {
  return yices_term_is_real(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsArithmetic(JNIEnv *env, jclass, jint x) {
  return yices_term_is_arithmetic(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsBitvector(JNIEnv *env, jclass, jint x) {
  return yices_term_is_bitvector(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsTuple(JNIEnv *env, jclass, jint x) {
  return yices_term_is_tuple(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsFunction(JNIEnv *env, jclass, jint x) {
  return yices_term_is_function(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsScalar(JNIEnv *env, jclass, jint x) {
  return yices_term_is_scalar(x);
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_termBitSize(JNIEnv *env, jclass, jint x) {
  return yices_term_bitsize(x);
}

// this one allocates auxiliary data structures
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsGround(JNIEnv *env, jclass, jint x) {
  try {
    return yices_term_is_ground(x);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
    return JNI_FALSE;
  }
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsAtomic(JNIEnv *env, jclass, jint x) {
  return yices_term_is_atomic(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsComposite(JNIEnv *env, jclass, jint x) {
  return yices_term_is_composite(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsProjection(JNIEnv *env, jclass, jint x) {
  return yices_term_is_projection(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsSum(JNIEnv *env, jclass, jint x) {
  return yices_term_is_sum(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsBvSum(JNIEnv *env, jclass, jint x) {
  return yices_term_is_bvsum(x);
}

JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_termIsProduct(JNIEnv *env, jclass, jint x) {
  return yices_term_is_product(x);
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_termConstructor(JNIEnv *env, jclass, jint x) {
  // we just return the Yices constructor code here
  return yices_term_constructor(x);
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_termNumChildren(JNIEnv *env, jclass, jint x) {
  return yices_term_num_children(x);
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_termChild(JNIEnv *env, jclass, jint x, jint idx) {
  return yices_term_child(x, idx);
}

/*
 * Collect all the children of term t
 * return NULL is t is not a valid term
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_termChildren(JNIEnv *env, jclass, jint t) {
#ifdef YICES_AT_LEAST_2_6_2
  term_vector_t aux;
  jintArray result = NULL;
  int32_t code;

  try {
    yices_init_term_vector(&aux);

    code = yices_term_children(t, &aux);
    if (code >= 0) {
      result = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_term_vector(&aux);

  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
#else
  return NULL;
#endif
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_termProjIndex(JNIEnv *env, jclass, jint x) {
  return yices_proj_index(x);
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_termProjArg(JNIEnv *env, jclass, jint x) {
  return yices_proj_arg(x);
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_boolConstValue(JNIEnv *env, jclass, jint x) {
  int32_t val;
  jint result;

  result = yices_bool_const_value(x, &val);
  if (result == 0) {
    result = val;
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_scalarConstantIndex(JNIEnv *env, jclass, jint x) {
  int32_t val;
  jint result;

  result = yices_scalar_const_value(x, &val);
  if (result == 0) {
    result = val;
  }
  return result;
}

JNIEXPORT jbooleanArray JNICALL Java_com_sri_yices_Yices_bvConstValue(JNIEnv *env, jclass, jint x) {
  jbooleanArray result = NULL;

  if (yices_term_constructor(x) == YICES_BV_CONSTANT) {
    int32_t n = yices_term_bitsize(x);

    assert(n >= 0);

    if (n <= 64) {
      // this should be the common case
      int32_t a[64];
      int32_t code = yices_bv_const_value(x, a);
      assert(code >= 0);
      result = convertToBoolArray(env, n, a);

    } else {
      try {
        int32_t *tmp =  new int32_t[n];
        int32_t code = yices_bv_const_value(x, tmp);
        assert(code >= 0);
        result = convertToBoolArray(env, n, tmp);
        delete [] tmp;
      } catch (std::bad_alloc) {
        out_of_mem_exception(env);
      }
    }
  }

  return result;
}


/*
 * Numerator of a rational constant x
 * - return NULL if x is not a rational constant
 * - return a byte array that contains the numerator otherwise
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_rationalConstNumAsBytes(JNIEnv *env, jclass, jint x) {
  jbyteArray result = NULL;
  mpq_t q;

  mpq_init(q);
  if (yices_rational_const_value(x, q) >= 0) {
    result = mpz_to_byte_array(env, mpq_numref(q));
  }
  mpq_clear(q);

  return result;
}


/*
 * Denominator as an array of bytes
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_rationalConstDenAsBytes(JNIEnv *env, jclass, jint x) {
  jbyteArray result = NULL;
  mpq_t q;

  mpq_init(q);
  if (yices_rational_const_value(x, q) >= 0) {
    result = mpz_to_byte_array(env, mpq_denref(q));
  }
  mpq_clear(q);

  return result;
}



/*
 * TERM NAMES
 */

/*
 * Give a a name to term t
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_setTermName(JNIEnv *env, jclass, jint t, jstring name) {
  jint code = -1;
  const char *s = env->GetStringUTFChars(name, NULL);

  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      code = yices_set_term_name(t, s);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(name, s);
  }

  return code;
}


/*
 * Remove the mapping from name to a term
 */
JNIEXPORT void JNICALL Java_com_sri_yices_Yices_removeTermName(JNIEnv *env, jclass, jstring name) {
  const char *s = env->GetStringUTFChars(name, NULL);

  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    yices_remove_term_name(s);
    env->ReleaseStringUTFChars(name, s);
  }
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_clearTermName(JNIEnv *env, jclass, jint t) {
  return yices_clear_term_name(t);
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_getTermName(JNIEnv *env, jclass, jint t) {
  return convertToString(env, yices_get_term_name(t));
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_getTermByName(JNIEnv *env, jclass, jstring name) {
  jint t = -1;
  const char *s = env->GetStringUTFChars(name, NULL);

  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      t = yices_get_term_by_name(s);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(name, s);
  }

  return t;
}

/*
 * Convert t to a string by calling the Yices pretty printer
 * We print into an array of 80 columns x 30 lines
 */
JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_termToString__III(JNIEnv *env, jclass, jint t, jint columns, jint lines) {
  char *s;
  jstring result;

  if (columns < 0) columns = 40;
  if (lines < 0) lines = 10;
  s = yices_term_to_string(t, columns, lines, 0);
  result = convertToString(env, s);
  yices_free_string(s); // this is safe even if s is NULL

  return result;
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_termToString__I(JNIEnv *env, jclass, jint t) {
  char *s;
  jstring result;

  s = yices_term_to_string(t, 80, 30, 0);
  result = convertToString(env, s);
  yices_free_string(s); // this is safe even if s is NULL

  return result;
}

/*
 * Parse s as term using the Yices syntax
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_parseTerm(JNIEnv *env, jclass, jstring s) {
  jint result = -1;
  const char *aux = env->GetStringUTFChars(s, NULL);

  if (aux == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_parse_term(aux);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(s, aux);
  }

  return result;
}


/*
 * Substitution defined by v[] and map[] applied to term t
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_substTerm(JNIEnv *env, jclass, jint t, jintArray v, jintArray map) {
  jint result = -1;
  jsize n = env->GetArrayLength(v);

  if (n == env->GetArrayLength(map)) {
    int32_t *vars = array2int32(env, v, NULL);
    int32_t *vals = array2int32(env, map, NULL);
    if (vars == NULL || vals == NULL) {
      out_of_mem_exception(env);
    } else {
      result = yices_subst_term(n, vars, vals, t);
    }
    if (vars != NULL) release_int32_elems(env, v, vars);
    if (vals != NULL) release_int32_elems(env, map, vals);
  }

  return result;
}

/*
 * Apply the substitution to all elements of array a
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_substTermArray(JNIEnv *env, jclass, jintArray a, jintArray v, jintArray map) {
  jint result = -1;
  jsize n = env->GetArrayLength(v);

  if (n == env->GetArrayLength(map)) {
    int32_t *vars = array2int32(env, v, NULL);
    int32_t *vals = array2int32(env, map, NULL);
    int32_t *terms = array2terms(env, a, NULL);
    jsize m = env->GetArrayLength(a);
    if (vars == NULL || vals == NULL || terms == NULL) {
      out_of_mem_exception(env);
    } else {
      result = yices_subst_term_array(n, vars, vals, m, terms);
      jint mode = result < 0 ? JNI_ABORT : 0;
      // copy the result back into a if the substitution worked
      // just release the array otherwise.
      release_term_elems(env, a, terms, mode);
    }
    if (vars != NULL) release_int32_elems(env, v, vars);
    if (vals != NULL) release_int32_elems(env, map, vals);
  }

  return result;
}


/*
 * GARBAGE COLLECTION
 */

// number of terms. The result is uint32 but it can be safely converted to int32.
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesNumTerms(JNIEnv *, jclass) {
  return yices_num_terms();
}

// number of types.
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesNumTypes(JNIEnv *, jclass) {
  return yices_num_types();
}

// increment the reference counter for term t. Yices may allocate memory.
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesIncrefTerm(JNIEnv *env, jclass, jint t) {
  int result;
  try {
    result = yices_incref_term(t);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

// decrement the reference counter: doesn't allocate memory
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesDecrefTerm(JNIEnv *, jclass, jint t) {
  return yices_decref_term(t);
}

// increment the reference counter of type tau.
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesIncrefType(JNIEnv *env, jclass, jint tau) {
  int result;
  try {
    result = yices_incref_type(tau);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

// decrement the reference counter of type tau.
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesDecrefType(JNIEnv *, jclass, jint tau) {
  return yices_decref_type(tau);
}

// number of terms with a positive reference counter.
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesNumPosrefTerms(JNIEnv *, jclass) {
  return yices_num_posref_terms();
}

// number of types with a postive reference counter.
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_yicesNumPosrefTypes(JNIEnv *, jclass) {
  return yices_num_posref_types();
}

// call the garbage collector
JNIEXPORT void JNICALL Java_com_sri_yices_Yices_yicesGarbageCollect(JNIEnv *env, jclass,
                                                                    jintArray rootTerms, jintArray rootTypes, jboolean keepNamed) {

  // rootTerms and rootTypes may be null.
  // GetArrayLength and GetIntArrayElements seg fault if the array is NULL
  jsize num_root_terms = 0;
  int32_t *root_terms = NULL;
  if (rootTerms != NULL) {
    num_root_terms = env->GetArrayLength(rootTerms);
    root_terms = array2terms(env, rootTerms, NULL);
    if (num_root_terms > 0 && root_terms == NULL) {
      out_of_mem_exception(env);
      return;
    }
  }

  jsize num_root_types = 0;
  int32_t *root_types = NULL;
  if (rootTypes != NULL) {
    num_root_types = env->GetArrayLength(rootTypes);
    root_types = array2types(env, rootTypes, NULL);
    if (num_root_types > 0 && root_types == NULL) {
      out_of_mem_exception(env);
      return;
    }
  }

  try {
    yices_garbage_collect(root_terms, num_root_terms, root_types, num_root_types, keepNamed);
  } catch (std::bad_alloc &a) {
    out_of_mem_exception(env);
  }

  if (root_types != NULL) {
    release_int32_elems(env, rootTypes, root_types);
  }
  if (root_terms != NULL) {
    release_int32_elems(env, rootTerms, root_terms);
  }
}




/*
 * CONTEXTS
 */

/*
 * Allocate a configuration descriptor:
 * - the descriptor is set to the default configuration
 */
JNIEXPORT jlong JNICALL Java_com_sri_yices_Yices_newConfig(JNIEnv *env, jclass) {
  jlong result = 0; // NULL pointer
  try {
    result = reinterpret_cast<jlong>(yices_new_config());
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}

/*
 * Deletion
 */
JNIEXPORT void JNICALL Java_com_sri_yices_Yices_freeConfig(JNIEnv *env, jclass, jlong config) {
  yices_free_config(reinterpret_cast<ctx_config_t*>(config));
}

/*
 * Set a configuration parameter:
 * - name = the parameter name
 * - value = the value
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_setConfig(JNIEnv *env, jclass, jlong config, jstring name, jstring value) {
  jint code = -1;
  const char *n = env->GetStringUTFChars(name, NULL);
  const char *v = env->GetStringUTFChars(value, NULL);
  if (n == NULL || v == NULL) {
    out_of_mem_exception(env);
  } else {
    // can't cause out-of-mem
    code = yices_set_config(reinterpret_cast<ctx_config_t*>(config), n, v);
  }
  if (n != NULL) env->ReleaseStringUTFChars(name, n);
  if (v != NULL) env->ReleaseStringUTFChars(value, v);

  return code;
}

/*
 * Set config to a default solver type or solver combination for the given logic
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_defaultConfigForLogic(JNIEnv *env, jclass, jlong config, jstring logic) {
  jint code = -1;
  const char *l = env->GetStringUTFChars(logic, NULL);
  if (l == NULL) {
    out_of_mem_exception(env);
  } else {
    // can't cause out-of-mem
    code = yices_default_config_for_logic(reinterpret_cast<ctx_config_t*>(config), l);
    env->ReleaseStringUTFChars(logic, l);
  }
  return code;
}

JNIEXPORT jlong JNICALL Java_com_sri_yices_Yices_newContext(JNIEnv *env, jclass, jlong config) {
  jlong result = 0; // NULL pointer

  try {
    result = reinterpret_cast<jlong>(yices_new_context(reinterpret_cast<ctx_config_t*>(config)));
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_freeContext(JNIEnv *env, jclass, jlong context) {
  yices_free_context(reinterpret_cast<context_t*>(context));
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_contextStatus(JNIEnv *env, jclass, jlong ctx) {
  return yices_context_status(reinterpret_cast<context_t*>(ctx));
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_resetContext (JNIEnv *env, jclass, jlong ctx) {
  yices_reset_context(reinterpret_cast<context_t*>(ctx));
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_push(JNIEnv *env, jclass, jlong ctx) {
  jint result = -1;
  try {
    result = yices_push(reinterpret_cast<context_t*>(ctx));
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_pop(JNIEnv *env, jclass, jlong ctx) {
  jint result = -1;

  try {
    result = yices_pop(reinterpret_cast<context_t*>(ctx));
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_contextEnableOption(JNIEnv *env, jclass, jlong ctx, jstring opt) {
  jint result = -1;
  const char *option = env->GetStringUTFChars(opt, NULL);

  if (opt == NULL) {
    out_of_mem_exception(env);
  } else {
    // can't cause out-of-mem
    result = yices_context_enable_option(reinterpret_cast<context_t*>(ctx), option);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_contextDisableOption(JNIEnv *env, jclass, jlong ctx, jstring opt) {
  jint result = -1;
  const char *option = env->GetStringUTFChars(opt, NULL);
  if (opt == NULL) {
    out_of_mem_exception(env);
  } else {
    result = yices_context_disable_option(reinterpret_cast<context_t*>(ctx), option);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_assertFormula(JNIEnv *env, jclass, jlong ctx, jint t) {
  jint result = -1;
  try {
    result = yices_assert_formula(reinterpret_cast<context_t*>(ctx), t);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_assertFormulas(JNIEnv *env, jclass, jlong ctx, jintArray t) {
  jsize n = env->GetArrayLength(t);
  term_t *a = array2terms(env, t, NULL);
  jint result = -1;

  if (a == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      result = yices_assert_formulas(reinterpret_cast<context_t*>(ctx), n, a);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    release_term_elems(env, t, a);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_checkContext(JNIEnv *env, jclass, jlong ctx, jlong params) {
  jint result = -1;

  try {
    result = yices_check_context(reinterpret_cast<context_t*>(ctx), reinterpret_cast<param_t*>(params));
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_assertBlockingClause(JNIEnv *env, jclass, jlong ctx) {
  jint result = -1;

  try {
    result = yices_assert_blocking_clause(reinterpret_cast<context_t*>(ctx));
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_stopSearch(JNIEnv *env, jclass, jlong ctx) {
  yices_stop_search(reinterpret_cast<context_t*>(ctx));
}

JNIEXPORT jlong JNICALL Java_com_sri_yices_Yices_newParamRecord(JNIEnv *env, jclass) {
  jlong result = 0; // NULL Pointer

  try {
    result = reinterpret_cast<jlong>(yices_new_param_record());
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_defaultParamsForContext(JNIEnv *env, jclass, jlong ctx, jlong params) {
  yices_default_params_for_context(reinterpret_cast<context_t*>(ctx), reinterpret_cast<param_t*>(params));
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_setParam(JNIEnv *env, jclass, jlong p, jstring pname, jstring value) {
  jint result = -1;
  const char *pnm = env->GetStringUTFChars(pname, NULL);
  const char *val = env->GetStringUTFChars(value, NULL);

  if (pnm == NULL || val == NULL) {
    out_of_mem_exception(env);
  } else {
    result = yices_set_param(reinterpret_cast<param_t*>(p), pnm, val);
  }
  if (pnm != NULL) env->ReleaseStringUTFChars(pname, pnm);
  if (val != NULL) env->ReleaseStringUTFChars(value, val);

  return result;
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_freeParamRecord(JNIEnv *env, jclass, jlong param) {
  yices_free_param_record(reinterpret_cast<param_t*>(param));
}


/*
 * MODELS
 */
JNIEXPORT jlong JNICALL Java_com_sri_yices_Yices_getModel(JNIEnv *env, jclass, jlong ctx, jint keep_subst) {
  jlong result = 0; // NULL pointer

  try {
    result = reinterpret_cast<jlong>(yices_get_model(reinterpret_cast<context_t*>(ctx), keep_subst));
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_freeModel(JNIEnv *env, jclass, jlong model) {
  yices_free_model(reinterpret_cast<model_t*>(model));
}

JNIEXPORT jlong JNICALL Java_com_sri_yices_Yices_modelFromMap(JNIEnv *env, jclass, jintArray var, jintArray map) {
  jsize vlen = env->GetArrayLength(var);
  jsize mlen = env->GetArrayLength(map);
  jlong result = 0; // NULL pointer

  if (vlen == mlen) {
    term_t *v = array2terms(env, var, NULL);
    term_t *m = array2terms(env, map, NULL);
    if (v == NULL || m == NULL) {
      out_of_mem_exception(env);
    } else {
      try {
        result = reinterpret_cast<jlong>(yices_model_from_map(vlen, v, m));
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
    }

    if (v != NULL) release_term_elems(env, var, v);
    if (m != NULL) release_term_elems(env, map, m);
  }

  return result;
}

// returns -1 for error, 0 for false, +1 for true
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_getBoolValue(JNIEnv *env, jclass, jlong model, jint t) {
  int32_t val = -1;
  jint err;

  try {
    err = yices_get_bool_value(reinterpret_cast<model_t*>(model), t, &val);
    if (err < 0) {
      val = -1;
    }
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return val;
}


/*
 * Value of term t in model, stored in a[0]
 * - returns 0 if this works, -1 for error.
 *
 * Possible errors:
 * - a is an empty array
 * - t is not valid or doesn't have an integer value small enough to
 *   fit in 64 bits
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_getIntegerValue(JNIEnv *env, jclass, jlong model, jint t, jlongArray a) {
  jlong  aux;
  jint result;

  result = -1;
  if (env->GetArrayLength(a) > 0) {
    // ugly cast because int64_t is (long long) and jlong is long
    try {
      assert(sizeof(int64_t) == sizeof(jlong));
      result = yices_get_int64_value(reinterpret_cast<model_t*>(model), t, reinterpret_cast<int64_t*>(&aux));
      if (result >= 0) {
        env->SetLongArrayRegion(a, 0, 1, &aux);
      }
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}


/*
 * Value of term t in model, returned as a rational a[0]/a[1]
 * - returns 0 if this works, -1 or -2 for error
 *
 * Possible errors:
 * - a has length 0 or 1
 * - any error reported by yices_get_rational64_value
 * - can also fail if then denominator (of type uint64_t) is too large to be
 *   converted to jlong (signed 64bits).
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_getRationalValue(JNIEnv *env, jclass, jlong model, jint t, jlongArray a) {
  int64_t num;
  uint64_t den;
  jlong aux[2];
  jint result;

  result = -1;
  if (env->GetArrayLength(a) >= 2) {
    try {
      result = yices_get_rational64_value(reinterpret_cast<model_t*>(model), t, &num, &den);
      if (result >= 0) {
        if (den <= static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
          aux[0] = num;
          aux[1] = (int64_t) den;
          env->SetLongArrayRegion(a, 0, 2, aux);
        } else {
          result = -2;
        }
      }
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}


/*
 * Value returned as a double
 * - returns 0 if this works, -1 for error.
 *
 * Possible errors:
 * - a is an empty array or error for yices
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_getDoubleValue(JNIEnv *env, jclass, jlong model, jint t, jdoubleArray a) {
  double aux;
  jint result;

  result = -1;
  if (env->GetArrayLength(a) > 0) {
    try {
      result = yices_get_double_value(reinterpret_cast<model_t*>(model), t, &aux);
      if (result >= 0) {
        env->SetDoubleArrayRegion(a, 0, 1, &aux);
      }
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}


/*
 * Value of term t in model, returned as a byte array.
 * This works if the value of t is an integer. Then the result can be used for converting to BigInteger.
 *
 * Return null if there's an error.
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_getIntegerValueAsBytes(JNIEnv *env, jclass, jlong model, jint t) {
  jbyteArray result = NULL;
  mpz_t z;

  try {
    mpz_init(z);
    if (yices_get_mpz_value(reinterpret_cast<model_t *>(model), t, z) >= 0) {
      result = mpz_to_byte_array(env, z);
    }
    mpz_clear(z);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}


/*
 * Value of term t assumed to be a rational.
 * - we return t's value in two steps: one call to get the numerator, one call to get the denominator.
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_getRationalValueNumAsBytes(JNIEnv *env, jclass, jlong model, jint t) {
  jbyteArray result = NULL;
  mpq_t q;

  try {
    mpq_init(q);
    if (yices_get_mpq_value(reinterpret_cast<model_t *>(model), t, q) >= 0) {
      result = mpz_to_byte_array(env, mpq_numref(q));
    }
    mpq_clear(q);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_getRationalValueDenAsBytes(JNIEnv *env, jclass, jlong model, jint t) {
  jbyteArray result = NULL;
  mpq_t q;

  try {
    mpq_init(q);
    if (yices_get_mpq_value(reinterpret_cast<model_t *>(model), t, q) >= 0) {
      result = mpz_to_byte_array(env, mpq_denref(q));
    }
    mpq_clear(q);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}

JNIEXPORT jbooleanArray JNICALL Java_com_sri_yices_Yices_getBvValue(JNIEnv *env, jclass, jlong model, jint t) {
  jbooleanArray result = NULL;
  uint32_t n = yices_term_bitsize(t);

  if (n > 0) {
    try {
      if (n <= 64) {
        int32_t a[64];
        int32_t code = yices_get_bv_value(reinterpret_cast<model_t *>(model), t, a);
        if (code >= 0) {
          result = convertToBoolArray(env, n, a);
        }
      } else {
        int32_t *tmp = new int32_t[n];
        int32_t code = yices_get_bv_value(reinterpret_cast<model_t *>(model), t, tmp);
        if (code >= 0) {
          result = convertToBoolArray(env, n, tmp);
        }
        delete[] tmp;
      }
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }


  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_getScalarValue(JNIEnv *env, jclass, jlong model, jint t) {
  int32_t val = -1;
  int32_t code;

  try {
    code = yices_get_scalar_value(reinterpret_cast<model_t*>(model), t, &val);
    if (code < 0) val = -1;
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return val;
}


JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valueAsTerm(JNIEnv *env, jclass, jlong model, jint t) {
  int32_t result = -1;

  try {
    result = yices_get_value_as_term(reinterpret_cast<model_t*>(model), t);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valuesAsTerms(JNIEnv *env, jclass, jlong model, jintArray input, jintArray output){
  jsize in;
  jsize on;
  term_t *itarr = NULL;
  term_t *otarr = NULL;
  int32_t code;
  in = env->GetArrayLength(input);
  if (in == 0) {
      return -1;
  }
  on = env->GetArrayLength(output);
  if (on < in) {
      return -2;
  }
  itarr = array2terms(env, input, NULL);
  otarr = new int[in];
  try {
	code = yices_term_array_value(reinterpret_cast<model_t*>(model), in, itarr, otarr);
	if (code == 0){
	  env->SetIntArrayRegion(output, 0, in, otarr);
	}
  } catch (std::bad_alloc &ba) {
	out_of_mem_exception(env);
  }
  delete otarr;
  release_term_elems(env, input, itarr);
  return code;
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_modelToString__JII(JNIEnv *env, jclass, jlong model, jint columns, jint lines) {
  char *s;
  jstring result = NULL;

  if (columns < 0) columns = 40;
  if (lines < 0) lines = 10;
  try {
    s = yices_model_to_string(reinterpret_cast<model_t*>(model), columns, lines, 0);
    result = convertToString(env, s);
    yices_free_string(s);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}

JNIEXPORT jstring JNICALL Java_com_sri_yices_Yices_modelToString__J(JNIEnv *env, jclass, jlong model) {
  char *s;
  jstring result = NULL;

  try {
    s = yices_model_to_string(reinterpret_cast<model_t*>(model), 80, std::numeric_limits<uint32_t>::max(), 0);
    result = convertToString(env, s);
    yices_free_string(s);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;
}


JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_hasDelegate(JNIEnv *env, jclass, jstring delegate){
#ifdef YICES_AT_LEAST_2_6_2
  jint code = 0;
  const char *s = env->GetStringUTFChars(delegate, NULL);
  if (s == NULL) {
    out_of_mem_exception(env);
  } else {
    try {
      code = yices_has_delegate(s);
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
    env->ReleaseStringUTFChars(delegate, s);
  }
  return (jboolean) code;
#else
return (jboolean) YICES_ERROR_REQUIRES_AT_LEAST_2_6_2;
#endif
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    checkFormula
 * Signature: (ILjava/lang/String;Ljava/lang/String;[J)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_checkFormula(JNIEnv *env, jclass, jint formula, jstring logic, jstring delegate, jlongArray marr){
#ifdef YICES_AT_LEAST_2_6_2
  int32_t code;
  const char *ds;
  const char *ls;
  jsize n;
  bool wantModel = false;
  model_t *model = NULL;
  jlong mdl;
  if (marr != NULL) {
    n = env->GetArrayLength(marr);
    if (n == 0) {
      return -1;
    }
    wantModel = true;
  }
  //delegate string
  ds = env->GetStringUTFChars(delegate, NULL);
  if (ds == NULL) {
    out_of_mem_exception(env);
  }
  //logic string
  ls = env->GetStringUTFChars(logic, NULL);
  if (ls == NULL) {
    env->ReleaseStringUTFChars(delegate, ds);
    out_of_mem_exception(env);
  }
  if (wantModel) {
    code = yices_check_formula(formula, ls, &model, ds);
    if (code == STATUS_SAT) {
      mdl = reinterpret_cast<jlong>(model);
      env->SetLongArrayRegion(marr, 0, 1, &mdl);
    }
  } else {
    code = yices_check_formula(formula, ls, NULL, ds);
  }
  env->ReleaseStringUTFChars(logic, ls);
  env->ReleaseStringUTFChars(delegate, ds);
  return code;
#else
  return YICES_ERROR_REQUIRES_AT_LEAST_2_6_2;
#endif
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    checkFormulas
 * Signature: ([ILjava/lang/String;Ljava/lang/String;[J)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_checkFormulas(JNIEnv *env, jclass, jintArray formulas, jstring logic, jstring delegate, jlongArray marr){
#ifdef YICES_AT_LEAST_2_6_2
  int32_t code;
  const char *ds = NULL;
  const char *ls = NULL;
  jsize n;
  bool wantModel = false;
  model_t *model = NULL;
  jlong mdl;
  term_t *tarr;
  if (marr != NULL) {
    n = env->GetArrayLength(marr);
    if (n == 0) {
      return -1;
    }
    wantModel = true;
    assert(n > 0);
  }
  n = env->GetArrayLength(formulas);
  if (n == 0) {
    return -2;
  }
  assert(n > 0);
  // term array
  tarr = array2terms(env, formulas, NULL);
  if (tarr == NULL) {
    out_of_mem_exception(env);
  }
  //delegate string
  ds = env->GetStringUTFChars(delegate, NULL);
  if (ds == NULL) {
    out_of_mem_exception(env);
  }
  //logic string
  ls = env->GetStringUTFChars(logic, NULL);
  if (ls == NULL) {
    env->ReleaseStringUTFChars(delegate, ds);
    out_of_mem_exception(env);
  }
  if (wantModel) {
    code = yices_check_formulas(tarr, n, ls, &model, ds);
    if (code == STATUS_SAT) {
      mdl = reinterpret_cast<jlong>(model);
      env->SetLongArrayRegion(marr, 0, 1, &mdl);
    }
  } else {
    code = yices_check_formulas(tarr, n, ls, NULL, ds);
  }
  env->ReleaseStringUTFChars(logic, ls);
  env->ReleaseStringUTFChars(delegate, ds);
  release_term_elems(env, formulas, tarr);
  return code;
#else
  return YICES_ERROR_REQUIRES_AT_LEAST_2_6_2;
#endif
}




/*
 * Class:     com_sri_yices_Yices
 * Method:    implicantForFormula
 * Signature: (JI)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_implicantForFormula(JNIEnv *env, jclass, jlong model, jint term){
  jintArray retval = NULL;
  int32_t code;
  term_vector_t aux;
  try {
    yices_init_term_vector(&aux);
    code = yices_implicant_for_formula(reinterpret_cast<model_t*>(model), term, &aux);
    if (code >= 0) {
      retval = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_term_vector(&aux);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return retval;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    implicantForFormulas
 * Signature: (J[I)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_implicantForFormulas(JNIEnv *env, jclass, jlong model, jintArray terms){
  jintArray retval = NULL;
  int32_t code;
  term_vector_t aux;
  term_t *tarr;
  jsize n = env->GetArrayLength(terms);
  if (n == 0) {
    //BD: should we force a yices error here?
    return retval;
  }
  assert(n > 0);
  tarr = array2terms(env, terms, NULL);
  if (tarr == NULL) {
    out_of_mem_exception(env);
    return retval;
  }
  try {
    yices_init_term_vector(&aux);
    code = yices_implicant_for_formulas(reinterpret_cast<model_t*>(model), n, tarr, &aux);
    if (code >= 0) {
      retval = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_term_vector(&aux);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  release_term_elems(env, terms, tarr);
  return retval;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    generalizeModel
 * Signature: (JI[II)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_generalizeModel__JI_3II(JNIEnv *env, jclass, jlong model, jint term, jintArray elims, jint mode){
  jintArray retval = NULL;
  int32_t code;
  term_vector_t aux;
  jsize n = env->GetArrayLength(elims);
  if (n == 0) {
    //BD: should we force a yices error here?
    return retval;
  }
  assert(n > 0);
  term_t *earr = array2terms(env, elims, NULL);
  if (earr == NULL) {
    out_of_mem_exception(env);
    return retval;
  }
  try {
    yices_init_term_vector(&aux);
    code = yices_generalize_model(reinterpret_cast<model_t*>(model), term, n, earr, static_cast<yices_gen_mode_t>(mode), &aux);
    if (code >= 0) {
      retval = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_term_vector(&aux);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  release_term_elems(env, elims, earr);
  return retval;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    generalizeModel
 * Signature: (J[I[II)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_generalizeModel__J_3I_3II(JNIEnv *env, jclass, jlong model, jintArray terms, jintArray elims, jint mode){
  jintArray retval = NULL;
  int32_t code;
  term_vector_t aux;
  jsize ne;
  term_t *earr = NULL;
  jsize nt;
  term_t *tarr = NULL;
  ne = env->GetArrayLength(elims);
  if (ne == 0) {
    return retval;
  }
  assert(ne > 0);
  earr = array2terms(env, elims, NULL);
  if (earr == NULL) {
    out_of_mem_exception(env);
    return retval;
  }
  nt = env->GetArrayLength(terms);
  if (nt == 0) {
    release_term_elems(env, elims, earr);
    return retval;
  }
  assert(nt > 0);
  tarr = array2terms(env, terms, NULL);
  if (tarr == NULL) {
    out_of_mem_exception(env);
    release_term_elems(env, elims, earr);
    return retval;
  }
  try {
    yices_init_term_vector(&aux);
    code = yices_generalize_model_array(reinterpret_cast<model_t*>(model), nt, tarr, ne, earr, static_cast<yices_gen_mode_t>(mode), &aux);
    if (code >= 0) {
      retval = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_term_vector(&aux);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  release_term_elems(env, terms, tarr);
  release_term_elems(env, elims, earr);
  return retval;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    exportToDimacs
 * Signature: (ILjava/lang/String;Z[I)I
 */
// returns 1 if the file was written, 0 if the formula is solved, or a negative number indicating an error, the smt_status is stored in status
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_exportToDimacs__ILjava_lang_String_2Z_3I(JNIEnv *env, jclass, jint formula, jstring filename, jboolean simplify, jintArray status){
#ifdef YICES_AT_LEAST_2_6_2
  int32_t code = -1;
  jsize n;
  const char *file = NULL;
  smt_status_t stat;
  n = env->GetArrayLength(status);
  if (n == 0) {
    return -1;
  }
  //filename string
  file = env->GetStringUTFChars(filename, NULL);
  if (file == NULL) {
    out_of_mem_exception(env);
  }
  code = yices_export_formula_to_dimacs(formula, file, simplify, &stat);
  if (code >=  0) {
    env->SetIntArrayRegion(status, 0, 1, reinterpret_cast<int32_t *>(&stat));
  }
  env->ReleaseStringUTFChars(filename, file);
  return code;
#else
  return YICES_ERROR_REQUIRES_AT_LEAST_2_6_2;
#endif
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    exportToDimacs
 * Signature: ([ILjava/lang/String;Z[I)I
 */
// returns 0 on success, or a negative numer indicating an error
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_exportToDimacs___3ILjava_lang_String_2Z_3I(JNIEnv *env, jclass, jintArray formulas, jstring filename, jboolean simplify, jintArray status){
#ifdef YICES_AT_LEAST_2_6_2
  int32_t code;
  jsize n;
  term_t *tarr;
  const char *file = NULL;
  smt_status_t stat;
  n = env->GetArrayLength(status);
  if (n == 0) {
    return -1;
  }
  n = env->GetArrayLength(formulas);
  if (n == 0) {
    return -2;
  }
  tarr = array2terms(env, formulas, NULL);
  if (tarr == NULL) {
    out_of_mem_exception(env);
  }
  //filename string
  file = env->GetStringUTFChars(filename, NULL);
  if (file == NULL) {
    out_of_mem_exception(env);
  }
  code = yices_export_formulas_to_dimacs(tarr, n, file, simplify, &stat);
  if (code >=  0) {
    env->SetIntArrayRegion(status, 0, 1, reinterpret_cast<int32_t *>(&stat));
  }
  env->ReleaseStringUTFChars(filename, file);
  release_term_elems(env, formulas, tarr);
  return code;
#else
  return YICES_ERROR_REQUIRES_AT_LEAST_2_6_2;
#endif
}


/*
 * Class:     com_sri_yices_Yices
 * Method:    getSupport
 * Signature: (JI)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_getSupport__JI(JNIEnv *env, jclass, jlong model, jint term){
#ifdef YICES_AT_LEAST_2_6_2
  term_vector_t aux;
  jintArray result = NULL;
  int32_t code;
  try {
    yices_init_term_vector(&aux);

    code = yices_model_term_support(reinterpret_cast<model_t*>(model), term, &aux);
    if (code >= 0) {
      result = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_term_vector(&aux);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
#else
  return NULL;
#endif
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    getSupport
 * Signature: (J[I)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sri_yices_Yices_getSupport__J_3I(JNIEnv *env, jclass, jlong model, jintArray terms){
#ifdef YICES_AT_LEAST_2_6_2
  term_vector_t aux;
  jintArray result = NULL;
  int32_t code;
  jsize n = env->GetArrayLength(terms);
  if (n == 0) {
    //BD: shuld we force a yices error here?
    return result;
  }
  assert(n > 0);
  term_t *tarr = array2terms(env, terms, NULL);
  if (tarr == NULL) {
    out_of_mem_exception(env);
    return result;
  }
  try {
    yices_init_term_vector(&aux);
    code = yices_model_term_array_support(reinterpret_cast<model_t*>(model), n, tarr, &aux);
    if (code >= 0) {
      result = convertToIntArray(env, aux.size, aux.data);
    }
    yices_delete_term_vector(&aux);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  release_term_elems(env, terms, tarr);
  return result;
#else
  return NULL;
#endif
}



static jobject makeYVal(JNIEnv *env, yval_t *yval){
  jclass yvalClass;
  jmethodID constructor;
  yvalClass = env->FindClass("com/sri/yices/YVal");
  assert(yvalClass != NULL);

  constructor = env->GetMethodID(yvalClass, "<init>", "(II)V");
  assert(constructor != NULL);
  return env->NewObject(yvalClass, constructor, yval->node_tag, yval->node_id);
}

//returns true if tag is OK, and fills in the slots of yval, false otherwise.
static bool convertToYval(jint tag, jint id, yval_t *yval){
  if ((tag <  YVAL_UNKNOWN) || (tag > YVAL_MAPPING)) {
    return false;
  }
  yval->node_tag = static_cast<yval_tag_t>(tag);
  yval->node_id = id;
  return true;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    getValue
 * Signature: (JI)Lcom/sri/yices/YVal;
 */
JNIEXPORT jobject JNICALL Java_com_sri_yices_Yices_getValue(JNIEnv *env, jclass, jlong model, jint term){
  yval_t yval;
  int32_t code;
  code = yices_get_value(reinterpret_cast<model_t*>(model), term, &yval);
  if (code == 0) {
    return makeYVal(env, &yval);
  }
  return NULL;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valIsInt
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_valIsInt(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  int32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return 0;
  } else {
    code = yices_val_is_int32(reinterpret_cast<model_t*>(model), &yval);
    return (jboolean)code;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valIsLong
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_valIsLong(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  int32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return 0;
  } else {
    code = yices_val_is_int64(reinterpret_cast<model_t*>(model), &yval);
    return (jboolean)code;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valIsInteger
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sri_yices_Yices_valIsInteger(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  int32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return 0;
  } else {
    code = yices_val_is_integer(reinterpret_cast<model_t*>(model), &yval);
    return (jboolean)code;
  }
}


/*
 * Class:     com_sri_yices_Yices
 * Method:    valBitSize
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valBitSize(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  uint32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_BV) {
    return 0;
  } else {
    code = yices_val_bitsize(reinterpret_cast<model_t*>(model), &yval);
    // code is not going to be truncated because of yices' hard coded limits.
    return (jint)code;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valTupleArity
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valTupleArity(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  uint32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_TUPLE) {
    return 0;
  } else {
    code = yices_val_tuple_arity(reinterpret_cast<model_t*>(model), &yval);
    // code is not going to be truncated because of yices' hard coded limits.
    return (jint)code;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valMappingArity
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valMappingArity(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  uint32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_MAPPING) {
    return 0;
  } else {
    code = yices_val_mapping_arity(reinterpret_cast<model_t*>(model), &yval);
    // code is not going to be truncated because of yices' hard coded limits.
    return (jint)code;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valFunctionArity
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valFunctionArity(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  uint32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_FUNCTION) {
    return 0;
  } else {
    code = yices_val_function_arity(reinterpret_cast<model_t*>(model), &yval);
    // code is not going to be truncated because of yices' hard coded limits.
    return (jint)code;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valFunctionType
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valFunctionType(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  int32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_FUNCTION) {
    return 0;
  } else {
    code = yices_val_function_type(reinterpret_cast<model_t*>(model), &yval);
    return (jint)code;
  }
}


/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetBool
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valGetBool(JNIEnv * env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  int32_t val;
  int32_t code;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_BOOL) {
    return -1;
  } else {
    val = 0;
    code = yices_val_get_bool(reinterpret_cast<model_t*>(model), &yval, &val);
    return code == 0 ? val : (jint)code ;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetInteger
 * Signature: (JII[J)I
 */
/*
 * Value of yval in model, stored in a[0]
 * - returns 0 if this works, -1 for error.
 *
 * Possible errors:
 * - yval isn't an integer
 * - a is an empty array
 * - yval is not valid or doesn't have an integer value small enough to
 *   fit in 64 bits
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valGetInteger(JNIEnv *env, jclass, jlong model, jint tag, jint id, jlongArray a){
  yval_t yval;
  jlong  aux;
  jint result;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return -1;
  } else {
    result = -2;
    if (env->GetArrayLength(a) > 0) {
      // ugly cast because int64_t is (long long) and jlong is long
      try {
        assert(sizeof(int64_t) == sizeof(jlong));
        result = yices_val_get_int64(reinterpret_cast<model_t*>(model), &yval, reinterpret_cast<int64_t*>(&aux));
        if (result >= 0) {
          env->SetLongArrayRegion(a, 0, 1, &aux);
        }
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
    }
    return result;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetRational
 * Signature: (JII[J)I
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valGetRational
  (JNIEnv *, jclass, jlong, jint, jint, jlongArray);
 */
/*
 * Value of yval in model, returned as a rational a[0]/a[1]
 * - returns 0 if this works, -1, -2, -3 for error
 *
 * Possible errors:
 * - yval is not a rational
 * - a has length 0 or 1
 * - any error reported by yices_val_get_rational64
 * - can also fail if then denominator (of type uint64_t) is too large to be
 *   converted to jlong (signed 64bits).
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valGetRational(JNIEnv *env, jclass, jlong model, jint tag, jint id, jlongArray a){
  yval_t yval;
  int64_t num;
  uint64_t den;
  jlong aux[2];
  jint result;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return -1;
  } else {
    result = -2;
    if (env->GetArrayLength(a) >= 2) {
      try {
        result = yices_val_get_rational64(reinterpret_cast<model_t*>(model), &yval, &num, &den);
        if (result >= 0) {
          if (den <= static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
            aux[0] = num;
            aux[1] = (int64_t) den;
            env->SetLongArrayRegion(a, 0, 2, aux);
          } else {
            result = -3;
          }
        }
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
    }
    return result;
  }
}


/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetDouble
 * Signature: (JII[D)I
 */
/*
 * Value returned as a double
 * - returns 0 if this works, -1, or -2 for error.
 *
 * Possible errors:
 * - yval isn't a double
 * - a is an empty array or error for yices
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valGetDouble(JNIEnv *env, jclass, jlong model, jint tag, jint id, jdoubleArray a){
  yval_t yval;
  double aux;
  jint result;
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return -1;
  } else {
    result = -2;
    if (env->GetArrayLength(a) > 0) {
      try {
        result = yices_val_get_double(reinterpret_cast<model_t*>(model), &yval, &aux);
        if (result >= 0) {
          env->SetDoubleArrayRegion(a, 0, 1, &aux);
        }
      } catch (std::bad_alloc &ba) {
        out_of_mem_exception(env);
      }
    }
    return result;
  }
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetBV
 * Signature: (JII)[Z
 */
JNIEXPORT jbooleanArray JNICALL Java_com_sri_yices_Yices_valGetBV(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  jbooleanArray result;
  uint32_t n;

  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_BV) {
    return NULL;
  }

  result = NULL;
  n = yices_val_bitsize(reinterpret_cast<model_t *>(model), &yval);

  if (n > 0) {
    try {
      if (n <= 64) {
        int32_t a[64];
        int32_t code = yices_val_get_bv(reinterpret_cast<model_t *>(model), &yval, a);
        if (code >= 0) {
          result = convertToBoolArray(env, n, a);
        }
      } else {
        int32_t *tmp = new int32_t[n];
        int32_t code = yices_val_get_bv(reinterpret_cast<model_t *>(model), &yval, tmp);
        if (code >= 0) {
          result = convertToBoolArray(env, n, tmp);
        }
        delete[] tmp;
      }
    } catch (std::bad_alloc &ba) {
      out_of_mem_exception(env);
    }
  }
  return result;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetScalar
 * Signature: (JII[I)I
 */
// Value (i.e., index) and type of a scalar or uninterpreted term
// return 0 on success. -1 or -2 if there's an error.
// -1 if the tag isn't kosher.
// -2 if the array a isn't of length 2
// -3 if the yval isn't of a scalar
// stores the scalar val in a[0] and the type in a[1]
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valGetScalar(JNIEnv *env, jclass, jlong model, jint tag, jint id, jintArray a){
  yval_t yval;
  int32_t result = -1;
  int32_t code;
  int32_t val;
  type_t tau;
  int32_t aux[2];
  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_SCALAR) {
    return result;
  }
  result = -2;
  if (env->GetArrayLength(a) != 2) {
    return result;
  }
  try {
    code = yices_val_get_scalar(reinterpret_cast<model_t*>(model), &yval, &val, &tau);
    if (code < 0) {
      result = -3;
    }
    aux[0] = val;
    aux[1] = (int)tau;
    env->SetIntArrayRegion(a, 0, 2, aux);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return val;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetIntegerAsBytes
 * Signature: (JII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_valGetIntegerAsBytes(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  jbyteArray result = NULL;
  mpz_t z;

  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return result;
  }

  try {
    mpz_init(z);
    if (yices_val_get_mpz(reinterpret_cast<model_t *>(model), &yval, z) >= 0) {
      result = mpz_to_byte_array(env, z);
    }
    mpz_clear(z);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }
  return result;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetRationalNumAsBytes
 * Signature: (JII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_valGetRationalNumAsBytes(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  jbyteArray result = NULL;
  mpq_t q;

  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return result;
  }

  try {
    mpq_init(q);
    if (yices_val_get_mpq(reinterpret_cast<model_t *>(model), &yval, q) >= 0) {
      result = mpz_to_byte_array(env, mpq_numref(q));
    }
    mpq_clear(q);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;

}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valGetRationalDenAsBytes
 * Signature: (JII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sri_yices_Yices_valGetRationalDenAsBytes(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  jbyteArray result = NULL;
  mpq_t q;


  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_RATIONAL) {
    return result;
  }
  try {
    mpq_init(q);
    if (yices_val_get_mpq(reinterpret_cast<model_t *>(model), &yval, q) >= 0) {
      result = mpz_to_byte_array(env, mpq_denref(q));
    }
    mpq_clear(q);
  } catch (std::bad_alloc &ba) {
    out_of_mem_exception(env);
  }

  return result;


}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valExpandTuple
 * Signature: (JII[Lcom/sri/yices/YVal;)I
 */
//iam: can I really get away without checking the tye of the children array?
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valExpandTuple(JNIEnv *env, jclass, jlong mdl, jint tag, jint id, jobjectArray children){
  yval_t yval;
  int32_t arity;
  jsize n;
  yval_t *carr;
  int32_t code;
  model_t *model = reinterpret_cast<model_t *>(mdl);
  int32_t i;
  yval_t ychild;
  jobject child;

  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_TUPLE) {
    return -1;
  }
  arity = yices_val_tuple_arity(model, &yval);
  if (arity == 0) {
    return -2;
  }
  n = env->GetArrayLength(children);
  if (n < arity) {
    return -3;
  }
  carr = new yval_t[arity];
  code = yices_val_expand_tuple(model, &yval, carr);
  if (code < 0) {
    delete carr;
    return -4;
  }
  for (i = 0; i < arity; i++) {
    ychild = carr[i];
    child = makeYVal(env, &ychild);
    env->SetObjectArrayElement(children, i, child);
  }
  delete carr;
  return 0;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valFunctionCardinality
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valFunctionCardinality(JNIEnv *env, jclass, jlong model, jint tag, jint id){
  yval_t yval;
  int32_t code;
  yval_t def;
  yval_vector_t mappings;
  jint retval = -1;

  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_FUNCTION) {
    return -1;
  }
  yices_init_yval_vector(&mappings);
  code = yices_val_expand_function(reinterpret_cast<model_t *>(model), &yval, &def, &mappings);
  if (code == 0) {
    retval = mappings.size;
  }
  yices_delete_yval_vector(&mappings);
  return retval;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valExpandFunction
 * Signature: (JII[Lcom/sri/yices/YVal;[Lcom/sri/yices/YVal;)I
*/
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valExpandFunction(JNIEnv *env, jclass cls, jlong mdl, jint tag, jint id,  jobjectArray def, jobjectArray mappings){
  yval_t yval;
  int32_t cardinality;
  jsize ndef;
  jsize nmap;
  int32_t code;
  yval_t ydef;
  yval_vector_t ymaps;
  int32_t i;

  if (!convertToYval(tag, id, &yval) ||  tag != YVAL_FUNCTION) {
    return -1;
  }
  cardinality = Java_com_sri_yices_Yices_valFunctionCardinality(env, cls, mdl, tag, id);
  if (cardinality <= 0) {
    return -2;
  }
  ndef = env->GetArrayLength(def);
  if (ndef < 1) {
    return -3;
  }

  nmap = env->GetArrayLength(mappings);
  if (nmap < cardinality) {
    return -4;
  }
  yices_init_yval_vector(&ymaps);
  code = yices_val_expand_function(reinterpret_cast<model_t *>(mdl), &yval, &ydef, &ymaps);
  if (code < 0) {
    return -5;
  }

  assert(static_cast<int32_t>(ymaps.size) == cardinality);

  env->SetObjectArrayElement(def, 0, makeYVal(env, &ydef));

  for (i = 0; i < cardinality; i++) {
    env->SetObjectArrayElement(mappings, i, makeYVal(env, &(ymaps.data[i])));
  }

  yices_delete_yval_vector(&ymaps);
  return 0;
}

/*
 * Class:     com_sri_yices_Yices
 * Method:    valExpandMapping
 * Signature: (JII[Lcom/sri/yices/YVal;[Lcom/sri/yices/YVal;)I
 */
JNIEXPORT jint JNICALL Java_com_sri_yices_Yices_valExpandMapping(JNIEnv *env, jclass, jlong mdl, jint tag, jint id, jobjectArray args, jobjectArray value){
  yval_t yval;
  int32_t arity;
  model_t *model = reinterpret_cast<model_t *>(mdl);
  yval_t yvalue;
  yval_t *yargs;
  jsize nargs;
  jsize n;
  int32_t code;
  int32_t i;

  if (!convertToYval(tag, id, &yval)  || tag != YVAL_MAPPING) {
    return -1;
  }
  arity = yices_val_mapping_arity(model, &yval);
  if (arity == 0) {
    return -2;
  }

  nargs = env->GetArrayLength(args);
  if (nargs < arity) {
    return -3;
  }

  n = env->GetArrayLength(value);
  if (n < 1) {
    return -4;
  }

  yargs = new yval_t[arity];
  code = yices_val_expand_mapping(model, &yval, yargs, &yvalue);

  if (code == 0) {
    env->SetObjectArrayElement(value, 0, makeYVal(env, &yvalue));

    for (i = 0; i < arity; i++) {
      env->SetObjectArrayElement(args, i, makeYVal(env, &(yargs[i])));
    }
  }
  delete yargs;
  return 0;
}


#if 0

JNIEXPORT void JNICALL Java_com_sri_yices_Yices_printModel(JNIEnv *env, jclass, jint f, jlong model) {
  // Figure out file descriptors later - for now, just print to stdout
  yices_print_model(stdout, reinterpret_cast<model_t*>(model));
}

#endif
