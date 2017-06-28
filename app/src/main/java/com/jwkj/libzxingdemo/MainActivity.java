package com.jwkj.libzxingdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.jwkj.libzxing.OnQRCodeScanCallback;
import com.jwkj.libzxing.QRCodeManager;
import com.jwkj.libzxingdemo.runtimepermissions.PermissionsManager;
import com.jwkj.libzxingdemo.runtimepermissions.PermissionsResultAction;
import com.jwkj.libzxingdemo.utils.QRCodeFileUtils;

public class MainActivity extends AppCompatActivity {
    private TextView controlLog;
    private ImageView qrCode;
    private Bitmap qrCodeBit;//生成的二维码
    private Context mContext;
    private EditText etContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requestPermission();//请求清单文件中所有的权限
    }

    /**
     * 初始化布局
     */
    private void initView() {
        mContext = this;
        controlLog = (TextView) findViewById(R.id.tv_control_log);
        qrCode = (ImageView) findViewById(R.id.iv_qrcode);
        etContent = (EditText) findViewById(R.id.et_content);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QRCodeFileUtils.saveBitmapFile(mContext, qrCodeBit);
                controlLog.append("\n\n(保存)二维码成功保存至 " + QRCodeFileUtils.KEY_QR_CODE_DIR);
            }
        });
    }

    public void onCreateQRLogo(View view) {
        createQRCode(true);
    }

    /**
     * 生成二维码
     *
     * @param view
     */
    public void onCreateQR(View view) {
        createQRCode(false);
    }

    private void createQRCode(boolean isLogo) {
        String content = etContent.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            content = "http://www.gwell.cc/";
        }

        /**
         * bitmap（即最后一个参数）设置为空时表示不添加logo
         */
        if (isLogo) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.logo);
            qrCodeBit = QRCodeManager.getInstance().createQRCode(content, 300, 300, bitmap);
        } else {
            qrCodeBit = QRCodeManager.getInstance().createQRCode(content, 300, 300);
        }
        qrCode.setImageBitmap(qrCodeBit);
    }

    /**
     * 进入扫描二维码页面
     *
     * @param view
     */
    public void onScanQR(View view) {
        QRCodeManager.getInstance()
                .with(this)
                .setReqeustType(1)
                .scanningQRCode(new OnQRCodeScanCallback() {
                    @Override
                    public void onCompleted(String result) {
                        controlLog.append("\n\n(结果)" + result);
                    }

                    @Override
                    public void onError(Throwable errorMsg) {
                        controlLog.append("\n\n(错误)" + errorMsg.toString());
                    }

                    @Override
                    public void onCancel() {
                        controlLog.append("\n\n(取消)扫描任务取消了");
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //注册onActivityResult
        QRCodeManager.getInstance().with(this).onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 适配android6.0以上权限                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         =
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
