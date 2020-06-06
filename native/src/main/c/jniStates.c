#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <stdbool.h>

#include <stdint.h>
#include <string.h> 

#include "include/uhcall.h"
#include "include/uhstateDB.h"

__attribute__((aligned(4096))) __attribute__((section(".data"))) uhstatedb_param_t uhcp;

void init(void *bufptr) {
  uhstatedb_param_t *ptr_uhcp = (uhstatedb_param_t *)bufptr;
  uhcall(UAPP_UHSTATEDB_FUNCTION_INIT, ptr_uhcp, sizeof(uhstatedb_param_t));
}

JNIEXPORT void JNICALL Java_org_opendaylight_l2switch_NativeStuff_initState(JNIEnv *env, jobject obj, jintArray maxArray, jint numStates) {
  jint *c_array;
  c_array = (*env)->GetIntArrayElements(env, maxArray, NULL);
  memcpy(&uhcp.maxArray, c_array, numStates);
  uhcp.vaddr = (uint32_t)&uhcp;
  init((void *)&uhcp);
  (*env)->ReleaseIntArrayElements(env, maxArray, c_array, 0);
}

void get(void *bufptr) {
  uhstatedb_param_t *ptr_uhcp = (uhstatedb_param_t *)bufptr;
  uhcall(UAPP_UHSTATEDB_FUNCTION_GET, ptr_uhcp, sizeof(uhstatedb_param_t));
}

JNIEXPORT jint JNICALL Java_org_opendaylight_l2switch_NativeStuff_getState(JNIEnv *env, jobject obj, jint IDnum) {
  jint out;
  uhcp.deviceID = IDnum;
  uhcp.vaddr = (uint32_t)&uhcp;
  get((void *)&uhcp);
  out = uhcp.stateVal;
  return out;
}

void next(void *bufptr) {
  uhstatedb_param_t *ptr_uhcp = (uhstatedb_param_t *)bufptr;
  uhcall(UAPP_UHSTATEDB_FUNCTION_NEXT, ptr_uhcp, sizeof(uhstatedb_param_t));
}

JNIEXPORT void JNICALL Java_org_opendaylight_l2switch_NativeStuff_transitionState(JNIEnv *env, jobject obj, jint IDnum) {
  uhcp.deviceID = IDnum;
  uhcp.vaddr = (uint32_t)&uhcp;
  next((void *)&uhcp);
}
  
