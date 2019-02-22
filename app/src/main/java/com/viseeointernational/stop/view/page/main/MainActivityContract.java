package com.viseeointernational.stop.view.page.main;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.view.page.BasePresenter;
import com.viseeointernational.stop.view.page.BaseView;

import java.util.List;

public interface MainActivityContract {

    interface View extends BaseView {

        void showEnableBluetooth();

        void showGuide();

        void showDevices(@NonNull List<Device> list, String checkedAddress);

        void showMovementCountChange(@NonNull String address, int count);

        void showDeviceConnectionChange(@NonNull String address, boolean isConnected);

        void showBlink();

        void showHeader(String s);

        void showName(String s);

        void showLastUpdate(String s);

        void showMonitoringEnable(boolean enable);

        void showState(String s);

        void showTime(String s);

        void showBattery(int i);

        void alertIfForceReset();

        void showDetail(String address);

        void showSetting(String address);

        void showAddNewDevice();
    }

    interface Presenter extends BasePresenter<View> {

        void result(int requestCode, int resultCode, Intent data);

        void checkDevice(@NonNull String address);

        void enableMonitoring(boolean enable);

        void reset(boolean forceReset);

        void showDetail();

        void showSetting();

        void addNewDevice();

        void appExit();
    }
}
