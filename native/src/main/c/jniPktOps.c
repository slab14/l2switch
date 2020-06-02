#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <string.h>

void reverse_data(char *buf, int len) {
    for(unsigned int i=0; i<len/2; ++i) {
        char tmp = buf[i];
        buf[i]=buf[len-1-i];
        buf[len-1-i]=tmp;
    }
}

//modify input 
JNIEXPORT void JNICALL Java_org_opendaylight_l2switch_NativeStuff_revData(JNIEnv *env, jobject obj, jstring javaString, jint len){
  if(len>0){
    const char *nativeString = (*env)->GetStringUTFChars(env, javaString, NULL);
    char * buff = malloc(sizeof(char) * len);
    memcpy(buff, nativeString, len);
    reverse_data(buff, len);
    (*env)->ReleaseStringUTFChars(env, javaString, (const char *)buff);
    free(buff);
  }
}

//return modified string
JNIEXPORT jstring JNICALL Java_org_opendaylight_l2switch_NativeStuff_rev(JNIEnv *env, jobject obj, jstring javaString, jint len){
  jstring result;
  if(len>0){
    const char *nativeString = (*env)->GetStringUTFChars(env, javaString, NULL);
    char * buff = malloc(sizeof(char) * len);
    memcpy(buff, nativeString, len);
    reverse_data(buff, len);
    result = (*env)->NewStringUTF(env,buff);
    free(buff);
  }
  return result;
}



