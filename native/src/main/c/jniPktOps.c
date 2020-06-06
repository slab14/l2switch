#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <string.h>
#include <sys/types.h>
#include <openssl/conf.h>
#include <openssl/evp.h>
#include <openssl/err.h>

#include <stdbool.h>
#include <stdint.h>
#include "include/uhcall.h"
#include "include/uagent.h"

__attribute__((aligned(4096))) __attribute__((section(".data"))) uagent_param_t uhcp_pkt;

void hypDecrypt(void *bufptr) {
  uagent_param_t *ptr_uhcp = (uagent_param_t *)bufptr;
  uhcall(UAPP_UAGENT_FUNCTION_SIGN, ptr_uhcp, sizeof(uagent_param_t));
}

JNIEXPORT jstring JNICALL Java_org_opendaylight_l2switch_NativeStuff_decrypt(JNIEnv *env, jobject obj, jbyteArray javaIn, jint len) {
  jstring result;
  if(len>0){
    jbyte *jbuffPtr = (*env)->GetByteArrayElements(env, javaIn, NULL);
    
    memcpy(&uhcp_pkt.pkt_data, jbuffPtr, len);
    uhcp_pkt.pkt_size=len;
    uhcp_pkt.vaddr = (uint32_t)&uhcp_pkt;
    uhcp_pkt.op=2;
    hypDecrpyt((void *) &uhcp_pkt);
    
    char outBuff[len];
    memcpy(outBuff, &uhcp_pkt.pkt_data, len);
    result = (*env)->NewStringUTF(env, outBuff);
    (*env)->ReleaseByteArrayElements(env, javaIn, jbuffPtr, JNI_ABORT);
  }
  return result;
}



