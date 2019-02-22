/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jwsd.libzxing.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.jwsd.libzxing.R;
import com.jwsd.libzxing.camera.CameraManager;
import com.jwsd.libzxing.decode.DecodeBitmap;
import com.jwsd.libzxing.decode.DecodeThread;
import com.jwsd.libzxing.utils.BeepManager;
import com.jwsd.libzxing.utils.CaptureActivityHandler;
import com.jwsd.libzxing.utils.InactivityTimer;
import com.jwsd.libzxing.utils.SelectAlbumUtils;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */

public class CaptureActivity extends Activity implements
        SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    public static final int RESULT_MULLT = 5;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    private SurfaceView scanPreview = null;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;
    private TextView capture_mask_bottom;

    private Rect mCropRect = null;
    private boolean isHasSurface = false;
    private ImageView ivBack, ivMullt;
    private int captureType = 0;
    private int textType = 0;
    private TextView tvAlbum;
    private static final int CODE_GALLERY_REQUEST = 101;

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    private DialogInterface.OnClickListener mOnClickListener;
    private ImageView iv_light;
    private RelativeLayout rl_device, rl_mule, rl_light;
    private TextView tx_title;
    private Camera camera;
    private Camera.Parameters parameter;
    private boolean isOpen = false;

    private boolean numberScanTwice = false;//扫描出数字重新扫描一次，第二次再扫描出数字就不再重新扫描

    private int scanCounts = 1;//第几次扫描

    private int supportDecodeType = DecodeThread.ALL_MODE;//支持扫描二维码的类型
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        setStatusColor(this,R.color.backgound_color);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_capture);
        captureType = getIntent().getIntExtra("type", 0);
        textType = getIntent().getIntExtra("textType", 0);
        numberScanTwice = getIntent().getBooleanExtra("numberScanTwice", false);
        supportDecodeType = getIntent().getIntExtra("supportDecodeType", DecodeThread.ALL_MODE);
        String string = getApplication().getResources().getString(R.string.jwstr_scan_it);
        scanCounts = 1;
//        Log.e("hdltag", "onCreate(CaptureActivity.java:102):" +string);
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        ivMullt = (ImageView) findViewById(R.id.iv_mudle);
        tvAlbum = (TextView) findViewById(R.id.tv_capture_select_album_jwsd);
        iv_light = (ImageView) findViewById(R.id.iv_light);
        tx_title = (TextView) findViewById(R.id.tx_title);
        rl_device = (RelativeLayout) findViewById(R.id.rl_device);
        rl_mule = (RelativeLayout) findViewById(R.id.rl_mule);
        rl_light = (RelativeLayout) findViewById(R.id.rl_light);
        capture_mask_bottom = (TextView) findViewById(R.id.capture_mask_bottom);
        if (textType != 0){
            capture_mask_bottom.setText(getString(R.string.jwstr_scan_device));
        }
        ivBack.setTag(123);
        ivMullt.setTag(124);
        tvAlbum.setTag(125);
        iv_light.setTag(126);
        tvAlbum.setOnClickListener(this);
        ivBack.setOnClickListener(this);
        ivMullt.setOnClickListener(this);
        iv_light.setOnClickListener(this);
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        if (captureType == 0) {
            rl_mule.setVisibility(View.VISIBLE);
            rl_device.setVisibility(View.GONE);
            ivMullt.setVisibility(View.VISIBLE);
        } else if (captureType == 1) {
            rl_mule.setVisibility(View.VISIBLE);
            rl_device.setVisibility(View.GONE);
            ivMullt.setVisibility(View.INVISIBLE);
        } else {
            rl_mule.setVisibility(View.GONE);
            rl_device.setVisibility(View.VISIBLE);
            tx_title.setText(getResources().getString(R.string.jwstr_prepare_device));
            capture_mask_bottom.setVisibility(View.GONE);
            rl_light.setVisibility(View.VISIBLE);
        }

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, -0.44f,
                Animation.RELATIVE_TO_PARENT, 0.56f);
        animation.setDuration(4500);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());
        handler = null;
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            scanPreview.getHolder().addCallback(this);
        }

        inactivityTimer.onResume();
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     * @param bundle    The extras
     */
    public void handleDecode(Result rawResult, Bundle bundle) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();
        if (captureType == 0) {
            Intent resultIntent = new Intent();
            bundle.putInt("width", mCropRect.width());
            bundle.putInt("height", mCropRect.height());
            bundle.putString("result", rawResult.getText());
            resultIntent.putExtras(bundle);
            this.setResult(RESULT_OK, resultIntent);
            CaptureActivity.this.finish();
        } else {
            if (numberScanTwice
                    && 1 == scanCounts
                    && SelectAlbumUtils.isNumeric(rawResult.toString())) {
                scanCounts++;
                handler = new CaptureActivityHandler(this, cameraManager, supportDecodeType);
                inactivityTimer.onResume();
            }else{
                scanDeviceSuccess(rawResult.toString(), bundle);
            }
        }

    }

    /**
     * 扫描设备二维码成功
     *
     * @param rawResult
     * @param bundle
     */
    private void scanDeviceSuccess(String rawResult, Bundle bundle) {
        Intent resultIntent = new Intent();
        bundle.putString("result", rawResult);
        resultIntent.putExtras(bundle);
        this.setResult(RESULT_OK, resultIntent);
        CaptureActivity.this.finish();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG,
                    "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            cameraManager.setCameraDisplayOrientation(this);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, supportDecodeType);
            }

            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.jwstr_prompt));
        builder.setMessage(getString(R.string.jwstr_camera_error));
        builder.setPositiveButton(getString(R.string.jwstr_confirm), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.jwstr_restart_preview, delayMS);
        }
    }

    public Rect getCropRect() {
        return mCropRect;
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onClick(View v) {
        switch (Integer.parseInt(v.getTag().toString())) {
            case 123:
                Intent resultIntent = new Intent();
                this.setResult(RESULT_CANCELED, resultIntent);
                CaptureActivity.this.finish();
                break;
            case 124:
                if (captureType == 0) {
                    Intent mullt = new Intent();
                    this.setResult(RESULT_MULLT, mullt);
                    CaptureActivity.this.finish();
                } else {
//                    Intent add = new Intent(this, AddContactActivity.class);
//                    CaptureActivity.this.startActivity(add);
//                    CaptureActivity.this.finish();
                    Intent resultInten1t = new Intent();
                    this.setResult(RESULT_CANCELED, resultInten1t);
                    CaptureActivity.this.finish();
                }
                break;
            case 125:
                //打开相册选择图片
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, CODE_GALLERY_REQUEST);
                break;
            case 126:
                Log.e("leleTest", "126");
                //获取到ZXing相机管理器创建的camera
                camera = cameraManager.getCamera();
                if (camera == null) {
                    return;
                }
                parameter = camera.getParameters();
                if (parameter == null) {
                    return;
                }
                //开灯
                if (isOpen) {
                    iv_light.setImageResource(R.drawable.light_off);
                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameter);
                    isOpen = false;
                } else {  // 关灯
                    iv_light.setImageResource(R.drawable.light_on);
                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameter);
                    isOpen = true;
                }
                break;
            default:
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CODE_GALLERY_REQUEST) {
            String picPath = SelectAlbumUtils.getPicPath(this, data);
            Result result = DecodeBitmap.scanningImage(picPath);
            if (result == null) {
                Toast.makeText(this, getString(R.string.jwstr_pic_no_qrcode), Toast.LENGTH_SHORT).show();
            } else {
                beepManager.playBeepSoundAndVibrate();
                String scanResult = DecodeBitmap.parseReuslt(result.toString());
                scanDeviceSuccess(scanResult, new Bundle());
            }
        }
    }

    //设备状态栏和标题栏一样的颜色
    public void setStatusColor(Activity activity, int resource) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            View statusView = createStatusView(activity, resource);
            // 添加 statusView 到布局中
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            decorView.addView(statusView);
        }
    }

    //生成一个和状态栏大小相同的矩形条
    public View createStatusView(Activity activity, int resource) {
        // 获得状态栏高度
        int statusBarHeight = getStatusHeigh();
        // 绘制一个和状态栏一样高的矩形
        View statusView = new View(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight);
        statusView.setLayoutParams(params);
        statusView.setBackgroundResource(resource);
        return statusView;
    }

    /**
     * 获取状态栏高度
     *
     * @return 状态栏高度
     */
    public int getStatusHeigh() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        return statusBarHeight;
    }

}