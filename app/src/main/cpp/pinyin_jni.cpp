#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <string>
#include <android/log.h>
#include "pinyinime.h"

#define TAG "PinyinJni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

using namespace ime_pinyin;

/**
 * Utility to convert jstring to char array
 */
bool jstring2char(JNIEnv* env, jstring str, char* out, size_t maxLength) {
    if (str == NULL) return false;
    jsize len = env->GetStringUTFLength(str);
    if (len >= (jsize)maxLength) return false;
    env->GetStringUTFRegion(str, 0, env->GetStringLength(str), out);
    out[len] = 0;
    return true;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_pinyin_PinyinIme_nativeOpenDecoder(JNIEnv *env, jclass clazz, jstring sys_dict, jstring usr_dict) {
    char c_sys_dict[256];
    char c_usr_dict[256];
    jstring2char(env, sys_dict, c_sys_dict, sizeof(c_sys_dict));
    jstring2char(env, usr_dict, c_usr_dict, sizeof(c_usr_dict));
    return im_open_decoder(c_sys_dict, c_usr_dict);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_pinyin_PinyinIme_nativeOpenDecoderFromAssets(JNIEnv *env, jclass clazz, jint fd, jlong start_offset, jlong length, jstring usr_dict) {
    char c_usr_dict[256];
    jstring2char(env, usr_dict, c_usr_dict, sizeof(c_usr_dict));
    // im_open_decoder_fd is available in AOSP pinyinime.h
    return im_open_decoder_fd(fd, start_offset, length, c_usr_dict);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pinyin_PinyinIme_nativeCloseDecoder(JNIEnv *env, jclass clazz) {
    im_close_decoder();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pinyin_PinyinIme_nativeResetSearch(JNIEnv *env, jclass clazz) {
    im_reset_search();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_pinyin_PinyinIme_nativeSearchAllNum(JNIEnv *env, jclass clazz, jstring key_word) {
    char c_key[256];
    jstring2char(env, key_word, c_key, sizeof(c_key));
    return im_search(c_key, strlen(c_key));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pinyin_PinyinIme_nativeEnableShmAsSzm(JNIEnv *env, jclass clazz, jboolean enable) {
    im_enable_shm_as_szm(enable);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pinyin_PinyinIme_nativeEnableYmAsSzm(JNIEnv *env, jclass clazz, jboolean enable) {
    im_enable_ym_as_szm(enable);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_pinyin_PinyinIme_nativeSearchAll(JNIEnv *env, jclass clazz, jstring key_word) {
    char c_key[256];
    if (!jstring2char(env, key_word, c_key, sizeof(c_key))) {
        return NULL;
    }
    
    size_t count = im_search(c_key, strlen(c_key));
    if (count > 80) count = 80; 
    
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize)count, stringClass, NULL);
    
    char16 cand_buf[128];
    for (size_t i = 0; i < count; i++) {
        char16* cand = im_get_candidate(i, cand_buf, 128);
        if (cand) {
            jstring s = env->NewString((const jchar*)cand, (jsize)utf16_strlen(cand));
            if (s) {
                env->SetObjectArrayElement(result, (jsize)i, s);
                env->DeleteLocalRef(s);
            }
        }
    }
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_pinyin_PinyinIme_nativeChoose(JNIEnv *env, jclass clazz, jint cand_id) {
    jint result = im_choose(cand_id);
    LOGD("nativeChoose: id=%d, returned fixed_len=%d", (int)cand_id, (int)result);
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_pinyin_PinyinIme_nativeGetCandidate(JNIEnv *env, jclass clazz, jint cand_id) {
    char16 cand_buf[128];
    char16* cand = im_get_candidate(cand_id, cand_buf, 128);
    if (cand) {
        return env->NewString((const jchar*)cand, (jsize)utf16_strlen(cand));
    }
    return NULL;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_pinyin_PinyinIme_nativeGetFixedLen(JNIEnv *env, jclass clazz) {
    return im_get_fixed_len();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pinyin_PinyinIme_nativeflushCache(JNIEnv *env, jclass clazz) {
    im_flush_cache();
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_pinyin_PinyinIme_nativeGetSpellingString(JNIEnv *env, jclass clazz) {
    size_t total_len;
    const char* spl_str = im_get_sps_str(&total_len);
    if (!spl_str) {
        LOGD("nativeGetSpellingString: spl_str is NULL");
        return NULL;
    }

    const uint16* spl_start;
    size_t spl_num = im_get_spl_start_pos(spl_start);
    size_t fixed_syllables = im_get_fixed_len();

    // Thoroughly unify units: map syllable index to character offset
    size_t char_offset = 0;
    if (fixed_syllables <= spl_num) {
        char_offset = spl_start[fixed_syllables];
    } else {
        char_offset = strlen(spl_str);
    }

    jstring raw_jstr = env->NewStringUTF(spl_str);

    std::string formatted;
    size_t raw_len = strlen(spl_str);
    size_t current_spl = 1; 
    for (size_t i = 0; i < raw_len; i++) {
        if (current_spl < spl_num && i == spl_start[current_spl]) {
            formatted += '\'';
            current_spl++;
        }
        formatted += spl_str[i];
    }
    jstring formatted_jstr = env->NewStringUTF(formatted.c_str());
    
    LOGD("PinyinJni: raw=%s, formatted=%s, fixed_syl=%zu, char_off=%zu", spl_str, formatted.c_str(), fixed_syllables, char_offset);

    jclass spellingStringClass = env->FindClass("com/pinyin/PinyinIme$SpellingString");
    jmethodID constructor = env->GetMethodID(spellingStringClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;I)V");
    
    return env->NewObject(spellingStringClass, constructor, formatted_jstr, raw_jstr, (jint)char_offset);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_pinyin_PinyinIme_nativeGetAllPredicts(JNIEnv *env, jclass clazz, jstring key_word) {
    char16 c_key[256];
    jsize len = env->GetStringLength(key_word);
    env->GetStringRegion(key_word, 0, len, (jchar*)c_key);
    c_key[len] = 0;
    
    char16 (*predicts)[kMaxPredictSize + 1];
    size_t count = im_get_predicts(c_key, predicts);
    
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize)count, stringClass, NULL);
    
    for (size_t i = 0; i < count; i++) {
        jstring s = env->NewString((const jchar*)predicts[i], (jsize)utf16_strlen(predicts[i]));
        env->SetObjectArrayElement(result, (jsize)i, s);
        env->DeleteLocalRef(s);
    }
    return result;
}