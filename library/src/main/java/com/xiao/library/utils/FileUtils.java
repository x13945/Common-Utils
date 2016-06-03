package com.xiao.library.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2015/10/9.
 */
public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();
    public static final String WORK_DIR_NAME = "b";

    public static boolean isSDCardWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static File getWorkDir(Context context) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + WORK_DIR_NAME);
        if (!file.exists()) {
            file.mkdirs();
        }

        return file;
    }

    public static File getManuscriptDraftDir(Context context) {
        File file = new File(getWorkDir(context) + File.separator + "draft");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
/*
    public static void saveImgClip2DCIM(final Application yoApp, String url){
        final String path = Environment.getExternalStorageDirectory().toString() + "/DCIM/";

        final String imgName = getPhotoFileName();
        NxOssTransferTaskManager ossManager = yoApp.getM_ossManager();
        final int WHAT_DOWNLOAD_SUCCESS = 1001;
        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case WHAT_DOWNLOAD_SUCCESS:{
                        yoApp.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                                .fromFile(new File((String) msg.obj))));
                        ToastUtils.showToast(yoApp, R.mipmap.img_success, R.string.title_msg_save_clip);
                        break;
                    }
                }
            }
        };

        String ossObjectKey;
        try {
            ossObjectKey = url.substring(url.indexOf(ossManager.getFolder()));
        } catch (StringIndexOutOfBoundsException e) {
            LogUtils.w(TAG, e);
            ToastUtils.showToast(yoApp, R.string.title_msg_save_clip_failed);
            return;
        }
        NxOssTransferTask.downloadClip(ossManager, ossObjectKey,
                new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
                    @Override
                    public void onSuccess(GetObjectRequest request, GetObjectResult result) {
                        // 请求成功
                        InputStream inputStream = result.getObjectContent();
                        FileOutputStream outputStream = null;
                        try {
                            File downloadFile = new File(path + imgName);
                            outputStream = new FileOutputStream(downloadFile);
                            byte[] buffer = new byte[2048];
                            int len;
                            while ((len = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, len);
                            }
                            Message message = Message.obtain();
                            message.what = WHAT_DOWNLOAD_SUCCESS;
                            message.obj = downloadFile.getPath();
                            handler.sendMessage(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(GetObjectRequest request, ClientException clientException, ServiceException serviceException) {
                        // 请求异常
                        if (clientException != null) {
                            // 本地异常如网络异常等
                            clientException.printStackTrace();
                        }
                        if (serviceException != null) {
                            // 服务异常
                            Log.e("ErrorCode", serviceException.getErrorCode());
                            Log.e("RequestId", serviceException.getRequestId());
                            Log.e("HostId", serviceException.getHostId());
                            Log.e("RawMessage", serviceException.getRawMessage());
                        }
                    }
                });
    }
*/

    public static String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }

    /**
     * 保存bitmap到/sdcard/Android/data/包名/cache/image 目录
     * @param context
     * @param bitmap
     * @return 文件路径
     */
    public static String saveCacheImage(Context context, Bitmap bitmap) {
        if (!isSDCardWritable()) {
            Log.e(TAG, "Can not write bitmap to SDCard, which SDCard is not available.");
            return null;
        }
        if (bitmap == null) {
            return null;
        }
        File imageCacheFolder = getImageCacheFolder(context);
        String imgName = "IMG_" + System.currentTimeMillis() + ".png";

        File cacheImage = new File(imageCacheFolder, imgName);
        FileOutputStream fOS = null;
        try {
            fOS = new FileOutputStream(cacheImage);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOS)) {
                fOS.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            cacheImage.delete();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            cacheImage.delete();
            return null;
        }finally {
            try {
                fOS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return cacheImage.getAbsolutePath();
    }

    @NonNull
    public static File getImageCacheFolder(Context context) {
        File imageCacheFolder = new File(context.getExternalCacheDir(), "image");
        if (!imageCacheFolder.exists()) {
            imageCacheFolder.mkdir();
        }
        return imageCacheFolder;
    }

    public static File getTempImageFile(Context context) {
        return new File(getImageCacheFolder(context), "temp.jpg");
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }

    public static File createNewFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        File newFile = new File(filePath);
        try {
            if (!newFile.exists()) {
                int slash = filePath.lastIndexOf('/');
                if (slash > 0 && slash < filePath.length() - 1) {
                    String dirPath = filePath.substring(0, slash);
                    File destDir = new File(dirPath);
                    if (!destDir.exists()) {
                        destDir.mkdirs();
                    }
                }
            } else {
                newFile.delete();
            }
            newFile.createNewFile();
        } catch (IOException e) {
            return null;
        }
        return newFile;
    }

    /**
     * @param filePath
     * @return
     * 检查文件是否存在
     */
    public static boolean checkFileExist(String filePath) {
        File file = new File(filePath);
        if(file.exists()){
            return true;
        }else{
            return false;
        }
    }

    public static String getPrefix(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            String name = file.getName();
            int dotIndex = name.indexOf(".");
            if (dotIndex < 1) {
                return "";
            } else {
                return name.substring(0, dotIndex);
            }
        } else {
            return "";
        }
    }

    public static String getSuffix(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            String name = file.getName();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex < 1) {
                return "";
            } else {
                return name.substring(dotIndex + 1, name.length());
            }
        } else {
            return "";
        }
    }

    public static void fileMove(String from, String to) throws Exception {// 移动指定文件夹内的全部文件
        try {
            File dir = new File(from);
            File[] files = dir.listFiles();// 将文件或文件夹放入文件集
            if (files == null)// 判断文件集是否为空
                return;
            File moveDir = new File(to);// 创建目标目录
            if (!moveDir.exists()) {// 判断目标目录是否存在
                moveDir.mkdirs();// 不存在则创建
            }
            for (int i = 0; i < files.length; i++) {// 遍历文件集
                if (files[i].isDirectory()) {// 如果是文件夹或目录,则递归调用fileMove方法，直到获得目录下的文件
                    fileMove(files[i].getPath(), to + "\\" + files[i].getName());// 递归移动文件
                    files[i].delete();// 删除文件所在原目录
                }
                File moveFile = new File(moveDir.getPath() + "\\"// 将文件目录放入移动后的目录
                        + files[i].getName());
                if (moveFile.exists()) {// 目标文件夹下存在的话，删除
                    moveFile.delete();
                }
                files[i].renameTo(moveFile);// 移动文件
                System.out.println(files[i] + " 移动成功");
            }
        } catch (Exception e) {
            throw e;
        }
    }

    // 复制目录下的文件（不包括该目录）到指定目录，会连同子目录一起复制过去。
    public static void copyFileFromDir(String toPath, String fromPath) {
        File file = new File(fromPath);
        createFile(toPath, false);// true:创建文件 false创建目录
        if (file.isDirectory()) {// 如果是目录
            copyFileToDir(toPath, listFile(file));
        }
    }

    // 复制目录到指定目录,将目录以及目录下的文件和子目录全部复制到目标目录
    public static void copyDir(String toPath, String fromPath) {
        File targetFile = new File(toPath);// 创建文件
        createFile(targetFile, false);// 创建目录
        File file = new File(fromPath);// 创建文件
        if (targetFile.isDirectory() && file.isDirectory()) {// 如果传入是目录
            copyFileToDir(targetFile.getAbsolutePath() + "/" + file.getName(),
                    listFile(file));// 复制文件到指定目录
        }
    }

    // 复制一组文件到指定目录。targetDir是目标目录，filePath是需要复制的文件路径
    public static void copyFileToDir(String toDir, String[] filePath) {
        if (toDir == null || "".equals(toDir)) {// 目录路径为空
            System.out.println("参数错误，目标路径不能为空");
            return;
        }
        File targetFile = new File(toDir);
        if (!targetFile.exists()) {// 如果指定目录不存在
            targetFile.mkdir();// 新建目录
        } else {
            if (!targetFile.isDirectory()) {// 如果不是目录
                System.out.println("参数错误，目标路径指向的不是一个目录！");
                return;
            }
        }
        for (int i = 0; i < filePath.length; i++) {// 遍历需要复制的文件路径
            File file = new File(filePath[i]);// 创建文件
            if (file.isDirectory()) {// 判断是否是目录
                copyFileToDir(toDir + "/" + file.getName(), listFile(file));// 递归调用方法获得目录下的文件
                System.out.println("复制文件 " + file);
            } else {
                copyFileToDir(toDir, file, "");// 复制文件到指定目录
            }
        }
    }

    public static void copyFileToDir(String toDir, File file, String newName) {// 复制文件到指定目录
        String newFile = "";
        if (newName != null && !"".equals(newName)) {
            newFile = toDir + "/" + newName;
        } else {
            newFile = toDir + "/" + file.getName();
        }
        File tFile = new File(newFile);
        copyFile(tFile, file);// 调用方法复制文件
    }

    public static void copyFileByShell(File dst, File src) {
        if (dst.exists()) {
            LogUtils.d(TAG, "文件" + dst.getAbsolutePath() + "已经存在，跳过该文件！");
            return;
        } else {
            String command = "cp " + src.getAbsolutePath() + " " + dst.getAbsolutePath();
            Process process = null;
            DataOutputStream os = null;
            try {
                process = Runtime.getRuntime().exec("sh");
                os = new DataOutputStream(process.getOutputStream());
                os.write(command.getBytes());
                os.writeBytes("\n");
                os.flush();

                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (process != null) {
                    process.destroy();
                }
            }
        }
    }

    public static void copyFile(File toFile, File fromFile) {// 复制文件
        if (toFile.exists()) {// 判断目标目录中文件是否存在
            System.out.println("文件" + toFile.getAbsolutePath() + "已经存在，跳过该文件！");
            return;
        } else {
            createFile(toFile, true);// 创建文件
        }
        System.out.println("复制文件" + fromFile.getAbsolutePath() + "到"
                + toFile.getAbsolutePath());
        try {
            InputStream is = new FileInputStream(fromFile);// 创建文件输入流
            FileOutputStream fos = new FileOutputStream(toFile);// 文件输出流
            byte[] buffer = new byte[1024];// 字节数组
            while (is.read(buffer) != -1) {// 将文件内容写到文件中
                fos.write(buffer);
            }
            is.close();// 输入流关闭
            fos.close();// 输出流关闭
        } catch (FileNotFoundException e) {// 捕获文件不存在异常
            e.printStackTrace();
        } catch (IOException e) {// 捕获异常
            e.printStackTrace();
        }
    }

    public static String[] listFile(File dir) {// 获取文件绝对路径
        String absolutPath = dir.getAbsolutePath();// 声获字符串赋值为路传入文件的路径
        String[] paths = dir.list();// 文件名数组
        String[] files = new String[paths.length];// 声明字符串数组，长度为传入文件的个数
        for (int i = 0; i < paths.length; i++) {// 遍历显示文件绝对路径
            files[i] = absolutPath + "/" + paths[i];
        }
        return files;
    }

    public static void createFile(String path, boolean isFile) {// 创建文件或目录
        createFile(new File(path), isFile);// 调用方法创建新文件或目录
    }

    public static void createFile(File file, boolean isFile) {// 创建文件
        if (!file.exists()) {// 如果文件不存在
            if (!file.getParentFile().exists()) {// 如果文件父目录不存在
                createFile(file.getParentFile(), false);
            } else {// 存在文件父目录
                if (isFile) {// 创建文件
                    try {
                        file.createNewFile();// 创建新文件
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    file.mkdir();// 创建目录
                }
            }
        }
    }

    public static final int MEDIA_TYPE_IMAGE = 1;

    public static Uri getOutputMediaFileUri(int type,Context mContext, String fileName){
        return Uri.fromFile(getOutputMediaFile(type,mContext,fileName));
    }

    @SuppressLint("ShowToast")
    public static File getOutputMediaFile(int type,Context mContext, String fileName){
        File mediaStorageDir=null;
        if(Environment.getExternalStorageState()!=null){
            mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "bxinwen/clip");
            if (! mediaStorageDir.exists()){
                if (! mediaStorageDir.mkdirs()){
                    Log.d("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
        }else{
            return null;
//            ToastUtils.showToast(mContext, "请插入SD卡");
//            mediaStorageDir=new File(mContext.getCacheDir(),"YoVideo");
//            if (! mediaStorageDir.exists()){
//                if (! mediaStorageDir.mkdirs()){
//                    Log.d("MyCameraApp", "failed to create directory");
//                    return null;
//                }
//            }
//            Log.d("MyCameraApp","路径为"+mediaStorageDir);
        }

//        String timeStamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +fileName + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static String combinePathAndSuffix(String path, String suffix) {
        return path + "." + suffix;
    }

    /**
     * @param file
     * 根据路径递归删除路径下所有的文件
     */
    public static void recursionDeleteFile(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                file.delete();
                return;
            }
            for (File f : childFile) {
                recursionDeleteFile(f);
            }
            file.delete();
        }
    }

    public static byte[] readStreamAsBytesArray(InputStream in, int readLength) throws IOException {
        if(in == null) {
            return new byte[0];
        } else {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];

            int len;
            for(long readed = 0L; readed < (long)readLength && (len = in.read(buffer, 0, Math.min(2048, (int)((long)readLength - readed)))) > -1; readed += (long)len) {
                output.write(buffer, 0, len);
            }

            output.flush();
            return output.toByteArray();
        }
    }

    public static byte[] readStreamAsBytesArray(RandomAccessFile in, int readLength) throws IOException {
        if(in == null) {
            return new byte[0];
        } else {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];

            int len;
            for(long readed = 0L; readed < (long)readLength && (len = in.read(buffer, 0, Math.min(2048, (int)((long)readLength - readed)))) > -1; readed += (long)len) {
                output.write(buffer, 0, len);
            }

            output.flush();
            return output.toByteArray();
        }
    }
}
