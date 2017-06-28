package com.jwkj.libzxingdemo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hdl.elog.ELog;
import com.jwkj.libzxing.activity.CaptureActivity;
import com.jwkj.libzxing.encoding.EncodingUtils;
import com.jwkj.libzxingdemo.runtimepermissions.PermissionsManager;
import com.jwkj.libzxingdemo.runtimepermissions.PermissionsResultAction;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int SCAN_REQUEST_CODE = 410;
    private TextView controlLog;
    private ImageView qrCode;
    private Bitmap qrCodeBit;//生成的二维码
    private Context mContext;
    private String filePath;//二维码保存路径
    private static final String KEY_QR_CODE_DIR = "/sdcard/qrcode/";//二维码保存路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        controlLog = (TextView) findViewById(R.id.tv_control_log);
        qrCode = (ImageView) findViewById(R.id.iv_qrcode);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBitmapFile(qrCodeBit);
            }
        });
        requestPermission();
    }


    /**
     * 通知相册图片增加
     *
     * @param fileName
     * @return
     */
    public boolean saveImgToGallery(String fileName) {
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
    public void saveBitmapFile(Bitmap bitmap) {
        File temp = new File(KEY_QR_CODE_DIR);//要保存文件先创建文件夹
        if (!temp.exists()) {
            temp.mkdir();
        }
        try {
            filePath = KEY_QR_CODE_DIR + "/QRCode_" + System.currentTimeMillis() + ".jpg";
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            Toast.makeText(this, "保存成功 " + filePath, Toast.LENGTH_SHORT).show();
            controlLog.append("\n\n保存成功：" + filePath);
            saveImgToGallery(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建二维码
     *
     * @param view
     */
    public void onCreateQR(View view) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.logo);
        /**
         * bitmap（即最后一个参数）设置为空时表示不添加logo
         */
        qrCodeBit = EncodingUtils.createQRCode("http://share.yoosee.co/share/?sharecode=879844565", 300, 300, bitmap);
        qrCode.setImageBitmap(qrCodeBit);
    }

    /**
     * 进入扫描二维码页面
     *
     * @param view
     */
    public void onScanQR(View view) {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra("type", 1);
        startActivityForResult(intent, SCAN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ELog.e("requestCode = " + requestCode + "\tresultCode = " + resultCode);
        if (requestCode == SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            String result = data.getStringExtra("result");
            if (TextUtils.isEmpty(result)) {
                controlLog.append("\n\nno result");
            } else {
                controlLog.append("\n\n" + result);
            }
        }
    }

    /**
     * 适配android6.0以上权限
     */
    private void requestPermission() {
        /**
         * 请求所有必要的权限
         */
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {
//				Toast.makeText(MainActivity.this, "All permissions have been granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(String permission) {
                //Toast.makeText(MainActivity.this, "Permission " + permission + " has been denied", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
