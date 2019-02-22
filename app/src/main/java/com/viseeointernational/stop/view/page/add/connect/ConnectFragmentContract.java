package com.viseeointernational.stop.view.page.add.connect;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.viseeointernational.stop.view.page.BasePresenter;
import com.viseeointernational.stop.view.page.BaseView;

public interface ConnectFragmentContract {

    interface View extends BaseView {

        void showEnableBluetooth();

        void showName(String s);

        void showHeader(String imagePath);

        void showHeader(Bitmap bitmap);

        void showCropHeader(Uri originHeader, Uri tempImage);

        void alertIfReconnect();

        void close();
    }

    interface Presenter extends BasePresenter<View> {

        void result(int requestCode, int resultCode, Intent data);

        void saveNameAndConnect(String name);
    }
}
