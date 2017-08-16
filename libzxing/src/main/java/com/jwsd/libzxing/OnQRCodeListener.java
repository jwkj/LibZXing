package com.jwsd.libzxing;

import android.content.Intent;

/**
 * Created by HDL on 2017/8/16.
 */

public abstract class OnQRCodeListener implements OnQRCodeScanCallback {
    /**
     * 当点击手动添加时回调
     */
    public void onManual(int requestCode, int resultCode, Intent data) {
    }
}
