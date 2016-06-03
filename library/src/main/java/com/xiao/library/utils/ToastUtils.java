package com.xiao.library.utils;

import android.content.Context;
import android.widget.Toast;


public class ToastUtils {

    public static void showToast(Context context, int titRes) {
        showToast(context, titRes, true);
    }

    public static void showToast(Context context, int titRes, boolean flag) {
        if(flag){
            Toast.makeText(context, titRes, Toast.LENGTH_SHORT).show();
        }
    }

    public static void showToast(Context context, String text) {
        showToast(context, text, true);
    }

    public static void showToast(Context context, String text, boolean flag) {
        if(flag){
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }
}