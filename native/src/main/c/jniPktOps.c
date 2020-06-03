#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h> 
#include <sys/types.h>
#include <openssl/conf.h>
#include <openssl/evp.h>
#include <openssl/err.h>


void handleErrors(void) {
    ERR_print_errors_fp(stderr);
    abort();
}

int decrypt(unsigned char *ciphertext, int ciphertext_len, unsigned char *key,
            unsigned char *iv, unsigned char *plaintext) {
    EVP_CIPHER_CTX *ctx;
    int len;
    int plaintext_len;

    /* Create and initialise the context */
    if(!(ctx = EVP_CIPHER_CTX_new()))
      handleErrors();

    // Initialise the decryption operation
    if(1 != EVP_DecryptInit_ex(ctx, EVP_aes_256_cbc(), NULL, key, iv))
      handleErrors();      

    // Provide the message to be decrypted, and obtain the plaintext output.
    if(1 != EVP_DecryptUpdate(ctx, plaintext, &len, ciphertext, ciphertext_len))
      handleErrors();      
    plaintext_len = len;

    // Finalise the decryption
    if(1 != EVP_DecryptFinal_ex(ctx, plaintext + len, &len))
      handleErrors();      
    plaintext_len += len;

    /* Clean up */
    EVP_CIPHER_CTX_free(ctx);

    return plaintext_len;
}

void reverse_data(char *buf, int len) {
    for(unsigned int i=0; i<len/2; ++i) {
        char tmp = buf[i];
        buf[i]=buf[len-1-i];
        buf[len-1-i]=tmp;
    }
}

//modify input -- results in error (double free/corruption)
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


JNIEXPORT jstring JNICALL Java_org_opendaylight_l2switch_NativeStuff_decrypt(JNIEnv *env, jobject obj, jbyteArray javaIn, jint len){
  jstring result;
  if(len>0){
    int decrypted_len=0;
    unsigned char *key=(unsigned char *)"_My sUpEr sEcrEt kEy 1234567890_";
    unsigned char *iv=(unsigned char *)"0123456789012345";
    jbyte *jbuffPtr = (*env)->GetByteArrayElements(env, javaIn, NULL);
    char * buff = malloc(sizeof(char) * len);
    char * decryptedBuff = malloc(sizeof(char) * len);
    
    memcpy(buff, jbuffPtr, len);
    decrypted_len = decrypt(buff, len, key, iv, decryptedBuff);
    char * outBuff = malloc(sizeof(char) * decrypted_len);
    memcpy(outBuff, decryptedBuff, decrypted_len);
    result = (*env)->NewStringUTF(env, outBuff);
    (*env)->ReleaseByteArrayElements(env, javaIn, jbuffPtr, JNI_ABORT);
    free(buff);
    free(decryptedBuff);    
  }
  return result;
}



