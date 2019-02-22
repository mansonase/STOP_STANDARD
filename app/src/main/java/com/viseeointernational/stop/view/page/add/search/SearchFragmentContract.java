package com.viseeointernational.stop.view.page.add.search;

import android.content.Intent;

import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.view.page.BasePresenter;
import com.viseeointernational.stop.view.page.BaseView;

import java.util.List;

public interface SearchFragmentContract {

    interface View extends BaseView {

        void showEnableBluetooth();

        void showDevices(List<Device> list);

        void showSearching();

        void stopSearching();

        void showConnect(String address);
    }

    interface Presenter extends BasePresenter<View> {

        void result(int requestCode, int resultCode, Intent data);

        void search();

        void selectDevice(Device device);
    }
}
