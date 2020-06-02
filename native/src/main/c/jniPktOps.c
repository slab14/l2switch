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

JNIEXPORT void JNICALL Java_org_opendaylight_l2switch_NativeStuff_revData(JNIEnv *env, jobject obj, jstring javaString, jint len){
  const char *nativeString = (*env)->GetStringUTFChars(env, javaString, NULL);
  char * buff = malloc(sizeof(char) * len);
  memcpy(buff, nativeString, len);
  printf("%s", buff);
  reverse_data(buff, len);
  printf("%s", buff);  
  (*env)->ReleaseStringUTFChars(env, javaString, (const char *)buff);
  free(buff)
}



