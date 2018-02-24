package com.oldfilm.gles;

import android.text.TextUtils;
import android.util.Log;

/**
 * LogUtils class.
 */
public class LogUtils {

    private static final boolean DEBUG = true;
    private static final String TAG = "old-film";

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void v(String msg) {
        if (!DEBUG || TextUtils.isEmpty(msg)) return;
        Log.v(TAG, msg);
    }

    public static void d(String msg) {
        if (!DEBUG || TextUtils.isEmpty(msg)) return;
        Log.d(TAG, msg);
    }

    public static void i(String msg) {
        if (!DEBUG || TextUtils.isEmpty(msg)) return;
        Log.i(TAG, msg);
    }

    public static void w(String msg) {
        if (!DEBUG || TextUtils.isEmpty(msg)) return;
        Log.w(TAG, msg);
    }

    public static void e(String msg) {
        if (!DEBUG || TextUtils.isEmpty(msg)) return;
        Log.e(TAG, msg);
    }
}
