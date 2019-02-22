package com.jwsd.libzxing;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.text.TextUtils;

import com.jwsd.libzxing.activity.CaptureActivity;
import com.jwsd.libzxing.decode.DecodeThread;
import com.jwsd.libzxing.encoding.EncodingUtils;

import java.io.IOException;


/**
 * 二维码管理器
 * Created by HDL on 2017/6/28.
 */

public class QRCodeManager extends IQRCodeStrategy {
    /**
     * 扫描请求码
     */
    private static final int SCAN_REQUEST_CODE = 410;
    private static QRCodeManager mQRCodeManager;
    private Activity context;
    private OnQRCodeListener callback;
    /**
     * 当前的请求码
     */
    private int curRequestCode = SCAN_REQUEST_CODE;
    /**
     * 请求的类型，默认为0（0：传感器添加 1：扫一扫 2：扫描自动配网）
     */
    private int requestType = 0;

    private int textType = 0;

    private boolean numberScanTwice = false;//扫描出数字重新扫描一次，第二次再扫描出数字就不再重新扫描

    private int supportDecodeType = DecodeThread.ALL_MODE;//支持扫描二维码的类型

    public enum SupportDecodeType{
        SUPPORT_BARCODE(0X100),//支持条形码
        SUPPORT_QRCODE(0X200),//支持二维码
        SUPPORT_ALL(0X300);//条形码和二维码都支持
        int value;

        SupportDecodeType(int type) {
            this.value = type;
        }
    }

    private QRCodeManager() {
    }

    public static QRCodeManager getInstance() {
        synchronized (QRCodeManager.class) {
            if (mQRCodeManager == null) {
                mQRCodeManager = new QRCodeManager();
            }
        }
        return mQRCodeManager;
    }

    /**
     * 设置请求码
     *
     * @param curRequestCode
     */
    public QRCodeManager setRequestCode(int curRequestCode) {
        this.curRequestCode = curRequestCode;
        return this;
    }

    /** 设置扫描结果为数字时需不需重新扫描
     * @param numberScanTwice
     * @return
     */
    public QRCodeManager setNumberScanTwice(boolean numberScanTwice){
        this.numberScanTwice = numberScanTwice;
        return this;
    }

    /** 设置支持扫描的图片类型
     * @param supportDecodeType：
     * @return
     */
    public QRCodeManager setSupportDecodeType(SupportDecodeType supportDecodeType){
        this.supportDecodeType = supportDecodeType.value;
        return this;
    }

    /**
     * 关联调用类
     *
     * @param context
     * @return
     */
    public QRCodeManager with(Activity context) {
        this.context = context;
        return this;
    }

    /**
     * 设置请求类型
     *
     * @param reqeustType
     * @return
     */
    public QRCodeManager setReqeustType(int reqeustType) {
        this.requestType = reqeustType;
        return this;
    }

    /**
     * 设置文本类型
     *
     * @param textType
     * @return
     */
    public QRCodeManager setTextType(int textType) {
        this.textType = textType;
        return this;
    }

    /**
     * <p>扫描二维码.</p>
     * 带回调的，一般表示结果由本管理器来处理onActivityResult方法，结果通过callback拿到。
     * <br/>
     * 此时，需要在activity/fragment的onActivityResult方法中注册{@link QRCodeManager#onActivityResult(int, int, Intent)}方法
     *
     * @return
     */
    public QRCodeManager scanningQRCode(OnQRCodeListener callback) {
        this.callback = callback;
        scanning(curRequestCode);
        return this;
    }

    /**
     * <p>扫描二维码.</p>
     * 不带回调的，一般表示自己处理onActivityResult方法
     *
     * @return
     */
    public QRCodeManager scanningQRCode(int requestCode) {
        scanning(requestCode);
        return this;
    }

    /**
     * 发起扫描
     *
     * @param requestCode
     */
    @Override
    void scanning(int requestCode) {
        this.curRequestCode = requestCode;
        Intent intent = new Intent(context, CaptureActivity.class);
        intent.putExtra("type", requestType);
        intent.putExtra("textType", textType);
        intent.putExtra("numberScanTwice", numberScanTwice);
        intent.putExtra("supportDecodeType", supportDecodeType);
        context.startActivityForResult(intent, curRequestCode);
    }

    /**
     * 结果回调
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        数据
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (callback == null) {
            return;
        }
        if (requestCode == curRequestCode && resultCode == Activity.RESULT_OK) {//成功
            String result = data.getStringExtra("result");
            if (TextUtils.isEmpty(result)) {
                callback.onError(new Throwable("result is null"));
            } else {
                //扫描成功发出提示音di
                AssetManager am = context.getAssets();// 获得该应用的AssetManager
                AssetFileDescriptor afd = null;
                try {
                    MediaPlayer player = new MediaPlayer();
                    afd = am.openFd("di.mp3");
                    player.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(), afd.getLength());
                    player.prepare(); // 准备
                    player.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                callback.onCompleted(result);
            }
        } else if (requestCode == curRequestCode && resultCode == Activity.RESULT_CANCELED) {//取消
            callback.onCancel();
        } else if (requestCode == curRequestCode && resultCode == CaptureActivity.RESULT_MULLT) {
            callback.onManual(requestCode, resultCode, data);
        }
    }

    /**
     * 创建二维码（不带logo）
     *
     * @param content   二维码的内容
     * @param widthPix  二维码的宽
     * @param heightPix 二维码的高
     * @return
     */
    @Override
    public Bitmap createQRCode(String content, int widthPix, int heightPix) {
        return EncodingUtils.createQRCode(content, widthPix, heightPix, null);
    }

    /**
     * 创建二维码（不带logo）
     *
     * @param content   二维码的内容
     * @param widthPix  二维码的宽
     * @param heightPix 二维码的高
     * @param logoBm    logo对应的bitmap对象
     * @return
     */
    @Override
    public Bitmap createQRCode(String content, int widthPix, int heightPix, Bitmap logoBm) {
        return EncodingUtils.createQRCode(content, widthPix, heightPix, logoBm);
    }
}
