package com.jwkj.libzxingdemo.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;


/**
 * Created by HDL on 2017/6/28.
 */

public class QRCodeFileUtils {
    /**
     * 二维码保存的文件夹
     */
    public static final String KEY_QR_CODE_DIR = "/sdcard/11qrcode/";//二维码保存路径
    /**
     * 通知相册图片增加
     *
     * @param fileName 文件全路径
     * @return
     */
    public static boolean nofityImgToGallery(Context mContext, String fileName) {
        if (fileName == null || fileName.length() <= 0)
            return false;
        String MimiType = "image/png";
        try {
            ContentValues values = new ContentValues();
            values.put("datetaken", new Date().toString());
            values.put("mime_type", MimiType);
            values.put("_data", fileName);
            ContentResolver cr = mContext.getContentResolver();
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MediaScannerConnection.scanFile(mContext,
                new String[]{KEY_QR_CODE_DIR}, null, null);
        return true;
    }

    /**
     * 保存图片到sdcard
     *
     * @param bitmap
     */
    public static void saveBitmapFile(Context mContext, Bitmap bitmap) {
        File temp = new File(KEY_QR_CODE_DIR);//要保存文件先创建文件夹
        if (!temp.exists()) {
            temp.mkdir();
        }
        try {
            String filePath = KEY_QR_CODE_DIR + "/QRCode_" + System.currentTimeMillis() + ".jpg";
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            Toast.makeText(mContext, "保存成功 " + filePath, Toast.LENGTH_SHORT).show();
            nofityImgToGallery(mContext, filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
