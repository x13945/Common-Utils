package com.xiao.library.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Hashtable;
import java.util.Map;


public class BitmapUtils {

    public static final int IMG_SCALE_640 = 640;
    public static final int IMG_SCALE_960 = 960;

    public static final String TAG = "BitmapManager";
    private static Map<String, Object> monitorMap = new Hashtable<String, Object>();

    public static final int IO_BUFFER_SIZE = 2 * 1024;


    /**
     * @param src
     * @return
     * n my testing, GIF images in 4.4 have transparency values as either white (-1) or black (-16777216).
     * After you load a Bitmap, you can convert the white/black pixels back to transparent.
     * Of course this will only work if the rest of the image doesn't use the same color.
     * If it does then you will also convert parts of your image to transparent that were not transparent in the original image.
     * In my case this wasn't a problem.
     */
    public static Bitmap eraseBGForSDK19(Bitmap src) {
        if (Build.VERSION.SDK_INT != 19) {
            return src;
        }
        src = eraseBG(src, -1);         // use for white background
        src = eraseBG(src, -16777216);  // use for black background
        return src;
    }

    @TargetApi(12)
    private static Bitmap eraseBG(Bitmap src, int color) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap b = src.copy(Config.ARGB_8888, true);
        b.setHasAlpha(true);

        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            if (pixels[i] == color) {
                pixels[i] = 0;
            }
        }

        b.setPixels(pixels, 0, width, 0, 0, width, height);

        return b;
    }


    public static String showLog() {
        long l2 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L;
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = Long.valueOf(l2);
        return l2 + "";
    }

    /**
     * 读取图片防止OOM
     *
     * @param path
     * @param maxH
     * @param maxW
     * @param context
     * @return
     */
    public static Bitmap loadBitmap(Uri path, int maxH, int maxW, Context context, Config config) {
        String srcPath = getRealPathFromURI(context, path);

        if (srcPath == null) {
            srcPath = path.getPath().toString();
        }
        return decodeFromPath(srcPath, maxH, maxW, config, true);
    }

    public static Bitmap loadBitmapWittOutHandle(Uri path, int maxH, int maxW, Context context, Config config) {

        String srcPath = getRealPathFromURI(context, path);
        if (srcPath == null) {
            srcPath = path.getPath().toString();
        }
        return decodeFromPath(srcPath, maxH, maxW, config, false);
    }

    public static Bitmap decodeFromPath(String path, int maxH, int maxW) {
        return decodeFromPath(path, maxH, maxW, null, false);
    }

    public static Bitmap decodeFromPath(
            String path, int maxH, int maxW, Config config, boolean readExif) {
        Bitmap bitmap = null;
        try {
            bitmap = captureImage(path, maxH, maxW, config, readExif);

        } catch (OutOfMemoryError localOutOfMemoryError1) {
            Log.w(TAG, "memory low!!! images:" + monitorMap);
            System.gc();
            Thread.yield();
            try {
                long l1 = System.currentTimeMillis();
                bitmap = captureImage(path, maxH, maxW, config, true);
                Log.d(TAG, "decode image " + path + ",cost " + (System.currentTimeMillis() - l1) + "ms");
            } catch (OutOfMemoryError localOutOfMemoryError2) {
                localOutOfMemoryError2.printStackTrace();
                Log.e(TAG, "Out of Memory on decode file " + path + "|" + showLog() + " images:" + monitorMap);
            }
        }
        return bitmap;
    }


    private static Bitmap captureImage(String path, int maxH, int maxW, Config config, boolean readExif) {
        if (path == null)
            return null;
        InputStream is = null;
        Bitmap bitmap = null;
        int degree = 0;
        try {
            is = new BufferedInputStream(new FileInputStream(path));//谁遇到过这问题 从HTC 那个新手机M8 里读取相册的图片 路径转换/document/image:19511
            Bitmap tempbitmap = getSampleSizeBitmap(is, path, maxH, maxW, config);
            if (readExif) {
                degree = BitmapUtils.getExifOrientation(path);
                bitmap = BitmapUtils.rotate(tempbitmap, degree);
            } else {
                bitmap = tempbitmap;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    /**
     * 获取压缩后的图片防止oom
     *
     * @param inputStream
     * @return
     */
    public static Bitmap getSampleSizeBitmap(InputStream inputStream, String path, int maxH, int maxW, Config config) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inInputShareable = true;

        int widthPixels = maxW;
        int heightPixels = maxH;
        options.inPreferredConfig = config;
        Bitmap bitmap = null;
        inputStream.mark(Integer.MAX_VALUE);
        options.inSampleSize = (int) Math.round(getOptRatio(
                inputStream, widthPixels, heightPixels));
        // 必须重置
        try {
            inputStream.reset();
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        } catch (IOException e) {
            e.printStackTrace();
            bitmap = BitmapFactory.decodeFile(path, options);
        }
        return bitmap;
    }


    /**
     * 获取缩放标准
     *
     * @param res
     * @return
     */
    public static int getScaleRate(Resources res) {
        DisplayMetrics localDisplayMetrics = res.getDisplayMetrics();
        int widthPixels = localDisplayMetrics.widthPixels;
        int heightPixels = localDisplayMetrics.heightPixels;
        if (widthPixels > heightPixels)
            heightPixels = widthPixels;
        if (heightPixels >= 800) {
            return IMG_SCALE_960;
        } else {
            return IMG_SCALE_640;
        }

    }

    /**
     * 通过Uri获取文件在本地存储的真实路径
     *
     * @return
     */
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        String scheme = contentUri.getScheme();
        if ("file".equals(scheme)) {
            return contentUri.getPath();
        }
        if (!"content".equals(scheme)) {
            if (scheme == null && new File(contentUri.getPath()).exists()) {
                return contentUri.getPath();
            }
            return null;
        }

        String[] proj = {Images.Media.DATA};
        Activity act = null;
        if (context instanceof Activity) {
            act = (Activity) context;
        }
        if (act == null) {
            return null;
        }
        Cursor cursor = act.managedQuery(contentUri, proj, // Which columns to return
                null, // WHERE clause; which rows to return (all rows)
                null, // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);

        //4.4下会去拿最近照片  uri取得方式
        if (result == null) {
            result = getPathByKITKAT(context, contentUri);
        }

        return result;
    }


    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @TargetApi(19)
    @SuppressLint("NewApi")
    public static String getPathByKITKAT(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
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
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    /**
     * 插入图片到相册
     *
     * @param context
     * @param filename 文件路径
     * @return
     */
    public static Uri exportToGallery(Context context, String filename) {
        // Save the name and description of a video in a ContentValues map.
        final ContentValues values = new ContentValues(2);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.MIME_TYPE, "image/png");
        values.put(Images.Media.DATA, filename);
        values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
        // Add a new record (identified by uri)
        final Uri uri = context.getContentResolver().insert(
                Images.Media.EXTERNAL_CONTENT_URI, values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + filename)));
        return uri;
    }

    /**
     * 获取一张图片所包含的像素数
     *
     * @return
     */
    public static long getImagePixes(InputStream is) {
        if (is == null) {
            return -1;
        }
        try {
            int b = is.read();
            if (b == 0xff) {
                b = is.read();
                if (b == 0xd8) {
                    return getJpgPixes(is);
                }
            } else if (b == 0x47) {
                b = is.read();
                if (b == 0x49) {
                    b = is.read();
                    if (b == 0x46) {
                        is.skip(3);//跳过3字节
                        int w = (is.read() | (is.read() << 8));
                        int h = (is.read() | (is.read() << 8));
                        return w * h;
                    }
                }
            } else if (b == 0x42) {
                b = is.read();
                if (b == 0x4d) {
                    is.skip(16);
                    long w = (is.read() | (is.read() << 8) | (is.read() << 16) | (is.read() << 24));
                    long h = (is.read() | (is.read() << 8) | (is.read() << 16) | (is.read() << 24));
                    return w * h;
                }

            } else if (b == 0x89) {
                b = is.read();
                if (b == 0x50) {
                    b = is.read();
                    if (b == 0x4E) {
                        is.skip(13);//跳过3字节
                        long w = ((is.read() << 24) | (is.read() << 16) | (is.read() << 8) | is.read());
                        long h = ((is.read() << 24) | (is.read() << 16) | (is.read() << 8) | is.read());
                        return w * h;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 获取 Jpg图片的像素数
     *
     * @param is
     * @return
     */
    private static long getJpgPixes(InputStream is) {
        int w = 0;
        int h = 0;
        int b = 0;
        try {
            while ((b = is.read()) != -1) {
                if (b == 0xff) {
                    b = is.read();
                    if (b >= 0xc0 && b <= 0xc3) {
                        is.skip(3);//跳过3字节
                        h = (is.read() << 8) | (is.read());
                        w = (is.read() << 8) | (is.read());
                        return w * h;
                    } else if (b != 0 && b != 0xd9 && b != 0xd8) {
                        int length = (is.read() << 8) | (is.read());
                        is.skip(length - 2);
                    } else if (b == -1) {
                        break;
                    }
                }
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 将给定图片维持宽高比缩放后，截取正中间的正方形部分。
     *
     * @param bitmap     原图
     * @param edgeLength 希望得到的正方形部分的边长
     * @return 缩放截取正中部分后的位图。
     */
    public static Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength) {
        if (null == bitmap || edgeLength <= 0) {
            return null;
        }

        Bitmap result = bitmap;
        int widthOrg = bitmap.getWidth();
        int heightOrg = bitmap.getHeight();

        if (widthOrg > edgeLength && heightOrg > edgeLength) {
            //压缩到一个最小长度是edgeLength的bitmap
            int longerEdge = (int) (edgeLength * Math.max(widthOrg, heightOrg) / Math.min(widthOrg, heightOrg));
            int scaledWidth = widthOrg > heightOrg ? longerEdge : edgeLength;
            int scaledHeight = widthOrg > heightOrg ? edgeLength : longerEdge;
            Bitmap scaledBitmap;

            try {
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
            } catch (Exception e) {
                return null;
            }

            //从图中截取正中间的正方形部分。
            int xTopLeft = (scaledWidth - edgeLength) / 2;
            int yTopLeft = (scaledHeight - edgeLength) / 2;

            try {
                result = Bitmap.createBitmap(scaledBitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
                scaledBitmap.recycle();
            } catch (Exception e) {
                return null;
            }
        }

        return result;
    }

    private static String CompressJpgFile(InputStream is, BitmapFactory.Options newOpts, String filePath, int degree, int rate) {
        Bitmap destBm = BitmapFactory.decodeStream(is, null, newOpts);
        destBm = rotate(destBm, degree);
        return CompressJpgFile(destBm, newOpts, filePath, rate);
    }

    private static String CompressJpgFile(Bitmap destBm, BitmapFactory.Options newOpts, String filePath, int rate) {
        //newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        if (destBm == null) {
            return null;
        } else {
            File destFile = FileUtils.createNewFile(filePath);

            // 创建文件输出流
            OutputStream os = null;
            try {
                os = new FileOutputStream(destFile);
                // 存储
                //int rate = 80;
                destBm.compress(CompressFormat.JPEG, rate, os);

            } catch (Exception e) {
                filePath = null;
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }
            return filePath;
        }
    }

    private static String CompressPngFile(InputStream is, BitmapFactory.Options newOpts, String filePath, int degree, int rate) {
        Bitmap destBm = BitmapFactory.decodeStream(is, null, newOpts);
        destBm = rotate(destBm, degree);
        return CompressPngFile(destBm, newOpts, filePath, rate);
    }

    private static String CompressPngFile(Bitmap destBm, BitmapFactory.Options newOpts, String filePath, int rate) {
        newOpts.inPreferredConfig = Config.ARGB_8888;
        if (destBm == null) {
            return null;
        } else {
            File destFile = FileUtils.createNewFile(filePath);

            // 创建文件输出流
            OutputStream os = null;
            try {
                os = new FileOutputStream(destFile);
                // 存储
                //int rate = 100;
                destBm.compress(CompressFormat.PNG, rate, os);

            } catch (Exception e) {
                filePath = null;
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }
            return filePath;
        }
    }

    private static long SIZE_200K = 200 * 1024;

    public static String compressImage4Zone(Context context, String srcPath, String destPath) {
        //InputStream is = null;
        File f = new File(srcPath);
        //is = new FileInputStream(f);
        String type = destPath.substring(destPath.lastIndexOf(".") + 1).toLowerCase();
        int degree = BitmapUtils.getExifOrientation(srcPath);
        if ("png".equals(type)) {
            return CompressPngFile4Zone(srcPath, destPath, degree);
        } else {
            return CompressJpgFile4Zone(srcPath, destPath, degree);
        }

    }

    private static int getSampleSizeByFileSize(long srcFileSize, long destFileSize) {
        int scale = (int) (srcFileSize / destFileSize);
        if (scale < 1) {
            return 1;
        }
        double d = Math.sqrt(scale);
        return (int) Math.ceil(d);
    }

    private static String CompressPngFile4Zone(String srcPath, String filePath, int degree) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = getSampleSizeByFileSize(new File(srcPath).length(), SIZE_200K);
        Bitmap destBm = BitmapFactory.decodeFile(srcPath, options);
        destBm = rotate(destBm, degree);
        return CompressPngFile(destBm, options, filePath, 75);
    }

    private static String CompressJpgFile4Zone(String srcPath, String filePath, int degree) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = getSampleSizeByFileSize(new File(srcPath).length(), SIZE_200K);
        Bitmap destBm = BitmapFactory.decodeFile(srcPath, options);
        destBm = rotate(destBm, degree);
        return CompressJpgFile(destBm, options, filePath, 75);
    }


    public static String compressImage(Context context, String srcPath, String destPath, int maxW, int maxH, int rate) {
        InputStream is = null;
        try {
            File f = new File(srcPath);
            BitmapFactory.Options newOpts = getSizeOpt(f, maxW, maxH);
            is = new FileInputStream(f);
            String type = destPath.substring(destPath.lastIndexOf(".") + 1).toLowerCase();
            int degree = BitmapUtils.getExifOrientation(srcPath);
            if ("png".equals(type)) {
                return CompressPngFile(is, newOpts, destPath, degree, rate);
            } else {
                return CompressJpgFile(is, newOpts, destPath, degree, rate);
            }

        } catch (Exception e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static String compressImage(Context context, String srcPath, String destPath, int maxW, int maxH) {
        return compressImage(context, srcPath, destPath, maxW, maxH, 100);
    }


    public static String compressImage(Context context, Bitmap bitmap, String destPath, int rate) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        String type = destPath.substring(destPath.lastIndexOf(".") + 1).toLowerCase();
        if ("png".equals(type)) {
            return CompressPngFile(bitmap, newOpts, destPath, rate);
        } else {
            return CompressJpgFile(bitmap, newOpts, destPath, rate);
        }
    }

    public static String compressImage(Context context, Bitmap bitmap, String destPath) {
        return compressImage(context, bitmap, destPath, 100);
    }


    /**
     * 先压缩图片大小
     *
     * @param file
     * @param maxWidth
     * @param maxHeight
     * @return
     * @throws IOException
     */
    public static BitmapFactory.Options getSizeOpt(File file, int maxWidth, int maxHeight) throws IOException {
        // 对图片进行压缩，是在读取的过程中进行压缩，而不是把图片读进了内存再进行压缩
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        InputStream is = new FileInputStream(file);
        double ratio = getOptRatio(is, maxWidth, maxHeight);
        is.close();
        newOpts.inSampleSize = (int) ratio;
        newOpts.inJustDecodeBounds = true;
        is = new FileInputStream(file);
        BitmapFactory.decodeStream(is, null, newOpts);
        is.close();
        int loopcnt = 0;
        while (newOpts.outWidth > maxWidth) {
            newOpts.inSampleSize += 1;
            is = new FileInputStream(file);
            BitmapFactory.decodeStream(is, null, newOpts);
            is.close();
            if (loopcnt > 3) break;
            loopcnt++;
        }
        newOpts.inJustDecodeBounds = false;
        return newOpts;
    }

    /**
     * 计算起始压缩比例
     * 先根据实际图片大小估算出最接近目标大小的压缩比例
     * 减少循环压缩的次数
     *
     * @param is
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static double getOptRatio(InputStream is, int maxWidth, int maxHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, opts);
        int srcWidth = opts.outWidth;
        int srcHeight = opts.outHeight;
        int destWidth = 0;
        int destHeight = 0;
        // 缩放的比例
        double ratio = 1.0;
        double ratio_w = 0.0;
        double ratio_h = 0.0;
        // 按比例计算缩放后的图片大小，maxLength是长或宽允许的最大长度
        if (srcWidth <= maxWidth && srcHeight <= maxHeight) {
            return ratio;   //小于屏幕尺寸时，不压缩
        }
        if (srcWidth > srcHeight) {
            ratio_w = srcWidth / maxWidth;
            ratio_h = srcHeight / maxHeight;
        } else {
            ratio_w = srcHeight / maxWidth;
            ratio_h = srcWidth / maxHeight;
        }
        if (ratio_w > ratio_h) {
            ratio = ratio_w;
        } else {
            ratio = ratio_h;
        }
        return ratio;
    }


    public enum ScalingLogic {
        CROP, FIT, SCALE_CROP
    }

    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;
            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int) (srcHeight * dstAspect);
                final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            } else {
                final int srcRectHeight = (int) (srcWidth / dstAspect);
                final int scrRectTop = (int) (srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        } else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }

    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
                                        ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float) srcWidth / (float) srcHeight;
            final float dstAspect = (float) dstWidth / (float) dstHeight;
            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int) (dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int) (dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }

    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight,
                                            ScalingLogic scalingLogic) {
        if (unscaledBitmap == null)
            return null;

        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        return scaledBitmap;
    }

    private static String getFileRoundSize(long paramLong) {
        return String.valueOf(Math.round(10.0F * ((float) paramLong / 1024.0F)) / 10.0F);
    }

    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, float zoomFactor,
                                            ScalingLogic scalingLogic) {
        if (unscaledBitmap == null)
            return null;
        int dstWidth = (int) (zoomFactor * unscaledBitmap.getWidth());
        int dstHeight = (int) (zoomFactor * unscaledBitmap.getHeight());

        return createScaledBitmap(unscaledBitmap, dstWidth, dstHeight, scalingLogic);
    }

    public static Bitmap getMutableBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isMutable()) {
            return bitmap;
        }

        try {
            File file = new File("/mutable.txt");
            file.getParentFile().mkdirs();

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, width * height * 4);
            bitmap.copyPixelsToBuffer(map);
            bitmap.recycle();

            bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            map.position(0);
            bitmap.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();

        } catch (Exception e) {

            return bitmap.copy(Config.ARGB_8888, true);
        }

        return bitmap;
    }
//// 2. inPurgeable 设定为 true，可以让java系统, 在内存不足时先行回收部分的内存  
//         options.inPurgeable = true;  
    // 与inPurgeable 一起使用
//         options.inInputShareable = true;

    /**
     * 获取图片文件头信息
     *
     * @param in
     * @return
     */
    public static BitmapFactory.Options getImageOptions(InputStream in) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, opts);
        return opts;
    }

    public static BitmapFactory.Options getImageOptions(File bitmap) {
        return getImageOptions(bitmap.getAbsolutePath());
    }

    public static BitmapFactory.Options getImageOptions(String bitmapPath) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(bitmapPath, opts);
        return opts;
    }

    /**
     * float angle 旋转角度 如果为零 顺时针转90°
     *
     * @param bitmap
     * @param angle
     * @return
     */
    public static Bitmap rotateAndFrame(Bitmap bitmap, float angle) {
        final double radAngle = Math.toRadians(angle);
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();
        final double cosAngle = Math.abs(Math.cos(radAngle));
        final double sinAngle = Math.abs(Math.sin(radAngle));
        final int width = (int) (bitmapHeight * sinAngle + bitmapWidth * cosAngle);
        final int height = (int) (bitmapWidth * sinAngle + bitmapHeight * cosAngle);
        final float x = (width - bitmapWidth) / 2.0f;
        final float y = (height - bitmapHeight) / 2.0f;
        final Bitmap decored = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        final Canvas canvas = new Canvas(decored);
        canvas.rotate(angle, width / 2.0f, height / 2.0f);
        canvas.drawBitmap(bitmap, x, y, null);

        return decored;
    }


    public static final Bitmap grey(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap faceIconGreyBitmap = Bitmap
                .createBitmap(width, height, Config.ARGB_8888);

        Canvas canvas = new Canvas(faceIconGreyBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(
                colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return faceIconGreyBitmap;
    }


    // Rotates the bitmap by the specified degree.
    // If a new bitmap is created, the original bitmap is recycled.
    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }

    // Rotates and/or mirrors the bitmap. If a new bitmap is created, the
    // original bitmap is recycled.
    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            if (mirror) {
                m.postScale(-1, 1);
                degrees = (degrees + 360) % 360;
                if (degrees == 0 || degrees == 180) {
                    m.postTranslate((float) b.getWidth(), 0);
                } else if (degrees == 90 || degrees == 270) {
                    m.postTranslate((float) b.getHeight(), 0);
                } else {
                    throw new IllegalArgumentException("Invalid degrees=" + degrees);
                }
            }

            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }

    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            Log.e("mq", "cannot read exif", ex);
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }

            }
        }
        return degree;
    }

    public static String restoreOrientation(String oldPath, String newPath) {
        try {
            ExifInterface oldExif = new ExifInterface(oldPath);
            ExifInterface newExif = new ExifInterface(newPath);
            int orientation = oldExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
            newExif.saveAttributes();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newPath;
    }

    public static Bitmap createRoundedCornerBitmap(Bitmap bitmap, float roundPx) {

        Bitmap roundCornerBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(roundCornerBitmap);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return roundCornerBitmap;
    }

    public static Bitmap blur(Context context, Bitmap original) {
        int radius = 5;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return blurBitmapByRenderScript(context, original, radius);
        } else {
            fastblur(original, 1f, radius);
        }
        return original;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static Bitmap blurBitmapByRenderScript(Context context, Bitmap bitmap, float radius){
        Bitmap overlay;
        try {
            overlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return blurBitmapByRenderScript(context,
                    createScaledBitmap(bitmap, 0.5f, ScalingLogic.CROP),
                    radius);
        }
        Canvas canvas = new Canvas(overlay);
        canvas.drawBitmap(bitmap, 0, 0, null);
        RenderScript rs = RenderScript.create(context);
        Allocation overlayAlloc = Allocation.createFromBitmap(rs, overlay);
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());
        blur.setInput(overlayAlloc);
        blur.setRadius(radius);
        blur.forEach(overlayAlloc);
        overlayAlloc.copyTo(overlay);
        rs.destroy();
        return overlay;
    }

    /**
     * Stack Blur v1.0 from
     * http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
     * Java Author: Mario Klingemann <mario at quasimondo.com>
     * http://incubator.quasimondo.com
     *
     * created Feburary 29, 2004
     * Android port : Yahel Bouaziz <yahel at kayenko.com>
     * http://www.kayenko.com
     * ported april 5th, 2012
     *
     * This is a compromise between Gaussian Blur and Box blur
     * It creates much better looking blurs than Box Blur, but is
     * 7x faster than my Gaussian Blur implementation.
     *
     * I called it Stack Blur because this describes best how this
     * filter works internally: it creates a kind of moving stack
     * of colors whilst scanning through the image. Thereby it
     * just has to add one new block of color to the right side
     * of the stack and remove the leftmost color. The remaining
     * colors on the topmost layer of the stack are either added on
     * or reduced by one, depending on if they are on the right or
     * on the left side of the stack.
     *
     * If you are using this algorithm in your code please add
     * the following line:
     * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
     */

    private static Bitmap fastblur(Bitmap sentBitmap, float scale, int radius) {

        int width = Math.round(sentBitmap.getWidth() * scale);
        int height = Math.round(sentBitmap.getHeight() * scale);
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }


    /**
     * 得到本地或者网络上的bitmap url - 网络或者本地图片的绝对路径,比如:
     *
     * A.网络路径: url="http://blog.foreverlove.us/girl2.png" ;
     *
     * B.本地路径:url="file://mnt/sdcard/photo/image.png";
     *
     * C.支持的图片格式 ,png, jpg,bmp,gif等等
     *
     * @param url
     * @return
     */
    public static Bitmap geetLocalOrNetBitmap(String url)
    {
        Bitmap bitmap = null;
        InputStream in = null;
        BufferedOutputStream out = null;
        try
        {
            in = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
            copy(in, out);
            out.flush();
            byte[] data = dataStream.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            data = null;
            return bitmap;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    /**
     * 将bitmap保存到内存卡中
     *
     * @param bitmap
     */
    public static void saveBitmapToSdcard(String path , Bitmap bitmap, int quality) {
        File file = new File(path);
        if (!file.exists()) {//如果目录不存在就创建目录
            file.delete();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            if (bitmap != null) {
                /*
                 * 三个参数的含义分别是：
                 * 1.保存图片的格式
                 * 2.标识图片质量0~100.质量越小压缩的越小（这里设置100标识不压缩）。另外如果图片是png格式，压缩是无损的，将忽略此参数（设置无效）
                 * 3.向OutputStream写入图片数据
                 */
                bitmap.compress(CompressFormat.JPEG, quality, out);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * @param imgPath
     * @return
     * 在不往内存加载图片的情况下获取图片的宽高
     */
    public static int[] getImageWh(String imgPath){
        BitmapFactory.Options options = new BitmapFactory.Options();

        /**
         * 最关键在此，把options.inJustDecodeBounds = true;
         * 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options); // 此时返回的bitmap为null
        /**
         *options.outHeight为原始图片的高
         */
        int[] result = new int[2];
        result[0] = options.outWidth;
        result[1] = options.outHeight;
        return result;
    }


}
