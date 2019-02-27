package com.viseeointernational.stop.view.page.add.connect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.source.device.DeviceSource;
import com.viseeointernational.stop.view.page.setting.SettingActivity;
import com.zhihu.matisse.Matisse;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

public class ConnectFragmentPresenter implements ConnectFragmentContract.Presenter {

    private static final String TAG = ConnectFragmentPresenter.class.getSimpleName();

    private ConnectFragmentContract.View view;

    private int reconnectCount;

    @Inject
    @Nullable
    String address;

    private Context context;
    private DeviceSource deviceSource;

    @Inject
    public ConnectFragmentPresenter(Context context, DeviceSource deviceSource) {
        this.context = context;
        this.deviceSource = deviceSource;
    }

    @Override
    public void takeView(ConnectFragmentContract.View view) {
        this.view = view;
        init();
    }

    private void init() {
        if (view != null) {
            view.showLoading();
        }
        deviceSource.getDevice(address, new DeviceSource.GetDeviceCallback() {
            @Override
            public void onDeviceLoaded(Device device) {
                if (view != null) {
                    view.cancelLoading();
                    view.showName(device.name);
                    view.showHeader(device.imagePath);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_device_not_available);
                }
            }
        });
    }

    @Override
    public void dropView() {
        view = null;
    }

    private File getHeaderDir() {
        File dir = new File("/mnt/sdcard/ViseeO/STOP AT-1/Header");
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    private Uri headerTempUri;

    @Override
    public void result(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ConnectFragment.REQUEST_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                }
                break;
            case SettingActivity.REQUEST_IMG:
                if (resultCode == Activity.RESULT_OK && view != null) {
                    List<Uri> ret = Matisse.obtainResult(data);
                    if (ret.size() > 0) {
                        headerTempUri = Uri.parse("file://" + "/" + getHeaderDir().getPath() + "/" + "temp.jpg");
                        view.showCropHeader(ret.get(0), headerTempUri);
                    }
                }
                break;
            case SettingActivity.REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(headerTempUri));
                        deviceSource.setHeader(address, bitmap);
                        if (view != null) {
                            view.showHeader(bitmap);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void saveNameAndConnect(String name) {
        if (TextUtils.isEmpty(name)) {
            if (view != null) {
                view.showMessage(R.string.msg_rename);
            }
            return;
        }
        if(name.length() > 30){
            if(view != null){
                view.showMessage(R.string.msg_name_too_long);
            }
            return;
        }
        deviceSource.setName(address, name);
        reconnectCount = 3;
        if (view != null) {
            view.showLoading();
        }
        doConnect();
    }

    private void doConnect() {
        deviceSource.connect(address, new DeviceSource.ConnectionCallback() {
            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.cancelLoading();
                    view.showEnableBluetooth();
                }
            }

            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected ");
                if (view != null) {
                    view.cancelLoading();
                    view.showMessage(R.string.msg_successful);
                    view.close();
                }
            }

            @Override
            public void onDisconnected() {
                reconnectCount--;
                Log.d(TAG, "onDisconnected " + reconnectCount);
                if (reconnectCount > 0) {
                    reconnect();
                    return;
                }
                if (view != null) {
                    view.cancelLoading();
                    view.alertIfReconnect();
                }
            }
        });
    }

    private void reconnect() {
        Observable.timer(3, TimeUnit.SECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        doConnect();
                    }
                });
    }
}