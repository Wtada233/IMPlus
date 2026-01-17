package com.pinyin;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * 谷歌输入法——词库功能封装
 */
public class PinyinIme {
    static {
        System.loadLibrary("pinyin");
    }

    public static class SpellingString {
        public String spellingStr;
        public String rawSpelling;
        public int decodedLen;

        public SpellingString(String spelling, String raw, int decodedLen) {
            this.spellingStr = spelling;
            this.rawSpelling = raw;
            this.decodedLen = decodedLen;
        }
    }

    private static native boolean nativeOpenDecoder(String sysDict, String usrDict);
    private static native boolean nativeOpenDecoderFromAssets(int file, long startOffset, long length, String usrDict);
    private static native void nativeCloseDecoder();
    private static native void nativeResetSearch();
    private static native int nativeSearchAllNum(String keyWord);
    private static native String[] nativeSearchAll(String keyWord);
    private static native int nativeChoose(int candId);
    private static native String nativeGetCandidate(int candId);
    private static native int nativeGetFixedLen();
    private static native void nativeflushCache();
    private static native SpellingString nativeGetSpellingString();
    private static native String[] nativeGetAllPredicts(String keyWord);

    private static native void nativeEnableShmAsSzm(boolean enable);
    private static native void nativeEnableYmAsSzm(boolean enable);

    private static final String TAG = "PinyinIme";
    private static boolean mOpenSucceeded = false;

    public static void init(Context context) {
        if (mOpenSucceeded) return;
        
        try {
            AssetManager am = context.getAssets();
            AssetFileDescriptor afd = am.openFd("gpinyindict/dict_pinyin32.dat.png");
            
            // Create a temporary user dictionary file path
            File userDictFile = new File(context.getFilesDir(), "usr_dict.dat");
            String userDictPath = userDictFile.getAbsolutePath();

            int fd = afd.getParcelFileDescriptor().detachFd();
            mOpenSucceeded = nativeOpenDecoderFromAssets(fd, afd.getStartOffset(), afd.getLength(), userDictPath);
            afd.close();
            
            if (mOpenSucceeded) {
                nativeEnableShmAsSzm(true);
                nativeEnableYmAsSzm(true);
            } else {
                Log.e(TAG, "Failed to open pinyin decoder from assets");
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException during pinyin init", e);
        }
    }

    public static String[] search(String pinyin) {
        if (!mOpenSucceeded) return new String[0];
        return nativeSearchAll(pinyin);
    }

    public static int choose(int candId) {
        if (!mOpenSucceeded) return 0;
        return nativeChoose(candId);
    }

    public static SpellingString getSpellingString() {
        if (!mOpenSucceeded) return null;
        return nativeGetSpellingString();
    }

    public static void resetSearch() {
        if (mOpenSucceeded) nativeResetSearch();
    }

    public static String[] getPredicts(String history) {
        if (!mOpenSucceeded) return new String[0];
        return nativeGetAllPredicts(history);
    }
}
