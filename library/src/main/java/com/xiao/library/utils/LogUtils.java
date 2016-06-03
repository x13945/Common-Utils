package com.xiao.library.utils;

import android.util.Log;

import com.xiao.library.BuildConfig;


/**
 * Created by Tianzilong on 2015/11/26.
 */
public class LogUtils {
    private static final boolean DEBUG = Log.isLoggable(LogUtils.class.getSimpleName(), Log.DEBUG);

    public static void d(String Tag, String msg){
        if(BuildConfig.DEBUG || DEBUG){
            Log.d(Tag, msg);
        }
    }

    public static void w(String Tag, String msg){
        if(BuildConfig.DEBUG || DEBUG){
            Log.w("LogUtils", msg);
        }
    }

    public static void w(String Tag, Throwable tr){
        if(BuildConfig.DEBUG || DEBUG){
            Log.d("LogUtils", Log.getStackTraceString(tr));
        }
    }

    public static void e(String Tag, String msg){
        if(BuildConfig.DEBUG || DEBUG){
            Log.e(Tag, msg);
        }
    }

    public static void i(String Tag, String msg){
        if(BuildConfig.DEBUG || DEBUG){
            Log.i(Tag, msg);
        }
    }

    public static void e(String tag, String msg, Exception e) {
        if(BuildConfig.DEBUG || DEBUG){
            Log.e(tag, msg, e);
        }
    }
}
