#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <stdbool.h>

int stateDB[100]={0};
int maxStateDB[100]={0};
bool set=false;

JNIEXPORT void JNICALL Java_org_opendaylight_l2switch_NativeStuff_initState(JNIEnv *env, jobject obj, jintArray maxArray, jint numStates) {
  if (!set){
    jint *c_array;
    c_array = (*env)->GetIntArrayElements(env, maxArray, NULL);
    int i;
    for(i=0; i<numStates; i++) {
      maxStateDB[i]=c_array[i];
    }
    (*env)->ReleaseIntArrayElements(env, maxArray, c_array, 0);
    set=true;
  }
}

JNIEXPORT jint JNICALL Java_org_opendaylight_l2switch_NativeStuff_getState(JNIEnv *env, jobject obj, jint IDnum) {
  return stateDB[IDnum];
}

bool canTransition(int IDnum){
  bool out=false;
  if(stateDB[IDnum]<maxStateDB[IDnum])
    out=true;
  return out;
}

JNIEXPORT void JNICALL Java_org_opendaylight_l2switch_NativeStuff_transitionState(JNIEnv *env, jobject obj, jint IDnum) {
  if(canTransition(IDnum)) 
    stateDB[IDnum]++;
}
  
