package com.viseeointernational.stop.view.page.add.search;

import android.app.Activity;
import android.content.Intent;

import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.source.device.DeviceSource;
import com.viseeointernational.stop.view.notification.Notifications;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

public class SearchFragmentPresenter implements SearchFragmentContract.Presenter {

    private static final String TAG = SearchFragmentPresenter.class.getSimpleName();

    private SearchFragmentContract.View view;

    private Map<String, Device> foundDevices = new LinkedHashMap<>();
    private DeviceSource deviceSource;

    private Notifications notifications;

    @Inject
    public SearchFragmentPresenter(DeviceSource deviceSource, Notifications notifications) {
        this.deviceSource = deviceSource;
        this.notifications = notifications;
    }

    @Override
    public void takeView(SearchFragmentContract.View view) {
        this.view = view;
        search();
    }

    @Override
    public void dropView() {
        deviceSource.stopSearch();
        if (view != null) {
            view.stopSearching();
        }
        view = null;
    }

    @Override
    public void result(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SearchFragment.REQUEST_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {

                }
                break;
        }
    }

    @Override
    public void search() {
        foundDevices.clear();
        if (view != null) {
            view.showDevices(new ArrayList<Device>(foundDevices.values()));
            view.showSearching();
        }
        deviceSource.search(20, new DeviceSource.SearchCallback() {
            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.stopSearching();
                    view.showEnableBluetooth();
                }
            }

            @Override
            public void onDeviceFound(Device device) {
                if (foundDevices.containsKey(device.address)) {
                    return;
                }
                foundDevices.put(device.address, device);
                if (view != null) {
                    view.showDevices(new ArrayList<Device>(foundDevices.values()));
                }
            }

            @Override
            public void onFinish() {
                if (view != null) {
                    view.stopSearching();
                    if (foundDevices.size() == 0) {
                        notifications.sendNoDeviceFoundNotification();
                    }
                }
            }
        });
    }

    @Override
    public void selectDevice(Device device) {
        Device device1 = new Device();
        device1.address = device.address;
        deviceSource.saveDevice(device1);
        if (view != null) {
            view.showConnect(device1.address);
        }
    }
}