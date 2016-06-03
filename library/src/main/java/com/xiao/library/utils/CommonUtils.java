package com.xiao.library.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class CommonUtils {
    public static final String TAG = CommonUtils.class.getSimpleName();
    public static final int TAG_SPAN_HANDLED_CLICK = -'h';

    public static final String WHISTLEROOT = "qu";
    public static final String VCARDDIRNAME = "vcard-images";
    public static final String CITY = "city";
    public static final String PRESENCE = "presence";
    public static final String JID = "jid";
    public static final String NOMEDIA = ".nomedia";


    public static String getGameFanrDir() {
        return Environment.getExternalStorageDirectory() + File.separator
                + WHISTLEROOT;
    }

    public static void hideMedia() {
        File file = new File(CommonUtils.getGameFanrDir(), NOMEDIA);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getGameFanrCityDir() {
        return getGameFanrDir() + File.separator + CITY;
    }

    public static String getAppIconPath(String url) {
        String fileName = url;
        if (fileName.contains("/")) {
            int index = url.lastIndexOf("/");
            fileName = url.substring(index + 1);
        }
        return getGameFanrDir() + File.separator + VCARDDIRNAME
                + File.separator + fileName;
    }

    public static String getAdbLoghDir() {
        return getGameFanrDir() + File.separator + "adb-log";
    }

    public static String getAnanImageDir() {
        String name = getGameFanrDir() + File.separator + "vcard-images" + File.separator;
        File file = new File(name);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    public static void setEmptyViewToListView(ListView lv, View view) {
        view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        view.setVisibility(View.GONE);
        ((ViewGroup) lv.getParent()).addView(view);
        lv.setEmptyView(view);
    }


    public static void fixBackgroundRepeat(View view) {
        Drawable bg = view.getBackground();
        if (bg != null) {
            if (bg instanceof BitmapDrawable) {
                BitmapDrawable bmp = (BitmapDrawable) bg;
                bmp.mutate(); // make sure that we aren't sharing state anymore
                bmp.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            }
        }
    }

    /**
     * 获取应用程序版本号
     *
     * @return
     */
    public static String getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pinfo;
        try {
            pinfo = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return pinfo.versionName;
    }


    public static int getVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pinfo;
        try {
            pinfo = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
        return pinfo.versionCode;
    }


    /**
     * 组装合适的图片URL(自动加上http://)
     *
     * @return
     */
    public static String concatUrl(String domain, String url) {
        if (url == null || domain == null) {
            return null;
        }
        String tempurl = "";
        try {
            tempurl = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        tempurl = tempurl.startsWith("http") ? tempurl : domain + tempurl;

        return tempurl;

    }


    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static void hideIME(Context context) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(context.INPUT_METHOD_SERVICE);
        if(imm.isActive()){
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void showIME(Context context, View v) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        // 得到InputMethodManager的实例
        // if (!imm.isActive()) {
        // //如果开启
        // imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,
        // InputMethodManager.HIDE_NOT_ALWAYS);
        // //关闭软键盘，开启方法相同，这个方法是切换开启与关闭状态的
        // }
        imm.showSoftInput(v, 0);

    }

    /**
     * 检查网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean checkNetworkAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    public static boolean isListViewLimited() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    public static String getUniqueId() {
        String id = "35" + //we make this look like a valid IMEI
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 digits
        return id;
    }


    public static int getStatusBarHeight(Context ctx) {
        int result = 0;
        int resourceId = ctx.getResources().getIdentifier("status_bar_height",
                "dimen", "android");
        if (resourceId > 0) {
            result = ctx.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 检测手机号是否合法
     */
    public static boolean checkPhoneNum(String phoneNum) {
        String pattern = "1[3458][0-9]{9}";
        return phoneNum.matches(pattern) || TextUtils.isEmpty(phoneNum);
    }

    /**
     * 检测邮箱是否合法
     */
    public static boolean checkEmail(String emailName) {
        String pattern = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        return emailName.matches(pattern) || TextUtils.isEmpty(emailName);
    }


    public static String getGenderString(int gender) {
        switch (gender) {
            case 1:
                return "男";
            case 0:
                return "女";
            case 2:
                return "女";
        }
        return "未知";
    }

    public static Intent getSelectImageUriIntent() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT < 19) {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        return intent;
    }

    public static void setListViewScrollSpeed(ListView...listViews) {
        if(listViews != null){
            for (ListView listView:listViews) {
                listView.setFriction(0.1f);
            }
        }
    }
}
