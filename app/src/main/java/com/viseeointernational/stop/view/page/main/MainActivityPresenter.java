package com.viseeointernational.stop.view.page.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.constant.StateType;
import com.viseeointernational.stop.data.constant.TimeFormatType;
import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.data.source.base.sharedpreferences.SharedPreferencesHelper;
import com.viseeointernational.stop.data.source.device.DeviceSource;
import com.viseeointernational.stop.data.source.location.LocationSource;
import com.viseeointernational.stop.util.TimeUtil;
import com.viseeointernational.stop.view.page.guide.GuideActivity;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

public class MainActivityPresenter implements MainActivityContract.Presenter {

    private static final String TAG = MainActivityPresenter.class.getSimpleName();

    private MainActivityContract.View view;

    private Context context;
    private DeviceSource deviceSource;
    private LocationSource locationSource;
    private SharedPreferencesHelper sharedPreferencesHelper;

    private String address;

    private boolean shouldClose = false;

    @Inject
    public MainActivityPresenter(Context context, DeviceSource deviceSource, LocationSource locationSource, SharedPreferencesHelper sharedPreferencesHelper) {
        this.context = context;
        this.deviceSource = deviceSource;
        this.locationSource = locationSource;
        this.sharedPreferencesHelper = sharedPreferencesHelper;
    }

    @Override
    public void takeView(final MainActivityContract.View view) {
        this.view = view;
        init();
    }

    private void init() {
        if (shouldClose) {
            shouldClose = false;
            if (view != null) {
                view.close();
            }
            return;
        }
        if (sharedPreferencesHelper.getIsFirstStart()) {
            if (view != null) {
                view.showGuide();
            }
            return;
        }
        if (!deviceSource.isBleEnable()) {
            if (view != null) {
                view.showEnableBluetooth();
            }
            return;
        }
        deviceSource.setDeviceCountChangeListener(new DeviceSource.DeviceCountChangeListener() {
            @Override
            public void onDeviceCountChange(@NonNull List<Device> devices) {
                address = null;
                if (devices.size() > 0) {
                    address = devices.get(0).address;
                }
                if (view != null) {
                    view.showDevices(devices, address);
                }
                if (!TextUtils.isEmpty(address)) {
                    checkDevice(address);
                }
            }
        });
        deviceSource.setDeviceConnectionChangeListener(new DeviceSource.DeviceConnectionChangeListener() {
            @Override
            public void onConnectionChange(@NonNull String address, boolean isConnected) {
                if (view != null) {
                    view.showDeviceConnectionChange(address, isConnected);
                }
            }
        });
        deviceSource.setMovementCountChangeListener(new DeviceSource.MovementCountChangeListener() {
            @Override
            public void onMovementCountChange(@NonNull String address, int count) {
                if (view != null) {
                    view.showMovementCountChange(address, count);
                }
            }
        });
        deviceSource.getPairedDevices(new DeviceSource.GetPairedDevicesCallback() {
            @Override
            public void onDevicesLoaded(List<Device> devices) {
                if (devices.size() == 0) {
                    sharedPreferencesHelper.setIsFirstStart(true);
                    if (view != null) {
                        view.showGuide();
                    }
                    return;
                }
                if (TextUtils.isEmpty(address) && devices.size() > 0) {
                    address = devices.get(0).address;
                }
                if (!TextUtils.isEmpty(address)) {
                    boolean exist = false;
                    for (int i = 0; i < devices.size(); i++) {
                        if (devices.get(i).address.equals(address)) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist && devices.size() > 0) {
                        address = devices.get(0).address;
                    }
                }
                if (view != null) {
                    view.showDevices(devices, address);
                }
                if (!TextUtils.isEmpty(address)) {
                    checkDevice(address);
                }
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.showEnableBluetooth();
                }
            }
        });
    }

    @Override
    public void dropView() {
        deviceSource.setDeviceCountChangeListener(null);
        deviceSource.setDeviceConnectionChangeListener(null);
        deviceSource.setMovementListener("", null);
        deviceSource.setBatteryListener("", null);
        deviceSource.setMovementCountChangeListener(null);
        view = null;
    }

    @Override
    public void result(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.REQUEST_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {

                }
                break;
            case MainActivity.REQUEST_GUIDE:
                if (resultCode == GuideActivity.RESULT_BACK) {
                    shouldClose = true;
                }
                break;
        }
    }

    @Override
    public void checkDevice(@NonNull String address) {
        this.address = address;
        deviceSource.setBatteryListener(address, new DeviceSource.BatteryListener() {
            @Override
            public void onPowerReceived(int power) {
                if (view != null) {
                    view.showBattery(power);
                }
            }
        });
        deviceSource.setMovementListener(address, new DeviceSource.MovementListener() {

            @Override
            public void onMovementReceived(@NonNull State state, @NonNull String timeFormat) {
                if (view != null) {
                    String s;
                    if (state.type == StateType.RESET) {
                        s = (String) context.getText(R.string.steady);
                        view.showState(s);
                        s = context.getText(R.string.since) + " " +
                                TimeUtil.getTime(state.time, timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
                        view.showTime(s);
                    } else {
                        s = (String) context.getText(R.string.movement_detected);
                        view.showState(s);
                        s = context.getText(R.string.at) + " " +
                                TimeUtil.getTime(state.time, timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
                        view.showTime(s);
                        view.showLastUpdate(context.getText(R.string.last_update) +
                                TimeUtil.getTime(state.time, timeFormat + "  " + TimeFormatType.TIME_DEFAULT));
                        view.showBlink();
                    }
                }
            }
        });
        if (view != null) {
            view.showBattery(0);
            view.showName("");
            view.showHeader("");
            view.showLastUpdate("");
            view.showMonitoringEnable(false);
            view.showState("");
            view.showTime("");
        }
        deviceSource.getDevice(address, new DeviceSource.GetDeviceCallback() {
            @Override
            public void onDeviceLoaded(Device device) {
                if (view != null) {
                    view.showName(device.name);
                    view.showHeader(device.imagePath);
                    view.showMonitoringEnable(device.enableMonitoring);
                    view.showBattery(device.battery);
                    if (device.lastUpdateTime != 0) {
                        view.showLastUpdate(context.getText(R.string.last_update) +
                                TimeUtil.getTime(device.lastUpdateTime, device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT));
                    }
                    String s;
                    if (device.currentState == null || device.currentState.type == StateType.RESET) {
                        s = (String) context.getText(R.string.steady);
                    } else {
                        s = (String) context.getText(R.string.movement_detected);
                    }
                    view.showState(s);
                    if (device.currentState == null) {
                        s = context.getText(R.string.since) + " " +
                                TimeUtil.getTime(Calendar.getInstance().getTimeInMillis(), device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
                    } else {
                        if (device.currentState.type == StateType.RESET) {
                            s = context.getText(R.string.since) + " " +
                                    TimeUtil.getTime(device.currentState.time, device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
                        } else {
                            s = context.getText(R.string.at) + " " +
                                    TimeUtil.getTime(device.currentState.time, device.timeFormat + "  " + TimeFormatType.TIME_DEFAULT);
                        }
                    }
                    view.showTime(s);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.showMessage(R.string.msg_device_not_available);
                }
            }
        });
    }

    @Override
    public void enableMonitoring(boolean enable) {
        deviceSource.enableMonitoring(address, enable);
    }

    @Override
    public void reset(final boolean forceReset) {
        if (forceReset) {
            saveReset(address, 0, 0);
        } else {
            if (view != null) {
                view.showLoading();
            }
            locationSource.getLocation(new LocationSource.LocationCallback() {
                @Override
                public void onLocationNotEnable() {
                    if (view != null) {
                        view.cancelLoading();
                        view.alertIfEnableLocation();
                    }
                }

                @Override
                public void onLocationLoaded(double latitude, double longitude) {
                    if (view != null) {
                        view.cancelLoading();
                    }
                    saveReset(address, latitude, longitude);
                }

                @Override
                public void onTimeOut() {
                    if (view != null) {
                        view.cancelLoading();
                        view.alertIfForceReset();
                    }
                }
            });
        }
    }

    private void saveReset(String address, double latitude, double longitude) {
        deviceSource.reset(address, latitude, longitude, new DeviceSource.ResetCallback() {
            @Override
            public void onSuccessful() {
                if (view != null) {
                    view.showMessage(R.string.msg_successful);
                }
            }

            @Override
            public void onBleNotAvailable() {
                if (view != null) {
                    view.showEnableBluetooth();
                }
            }

            @Override
            public void onDeviceDisconnected() {
                if (view != null) {
                    view.showMessage(R.string.msg_device_offline);
                }
            }

            @Override
            public void onDeviceNotAvailable() {
                if (view != null) {
                    view.showMessage(R.string.msg_device_not_available);
                }
            }

            @Override
            public void onAlreadyReset() {
                if (view != null) {
                    view.showMessage(R.string.msg_already_reset);
                }
            }

            @Override
            public void onError() {
                if (view != null) {
                    view.showMessage(R.string.msg_failed);
                }
            }
        });
    }

    @Override
    public void showDetail() {
        if (view != null) {
            if (address == null) {
                view.showMessage(R.string.msg_device_not_available);
            } else {
                view.showDetail(address);
            }
        }
    }

    @Override
    public void showSetting() {
        if (view != null) {
            if (address == null) {
                view.showMessage(R.string.msg_device_not_available);
            } else {
                view.showSetting(address);
            }
        }
    }

    @Override
    public void addNewDevice() {
        deviceSource.getPairedDeviceCount(new DeviceSource.GetPairedDeviceCountCallback() {
            @Override
            public void onCountLoaded(int count) {
                if (count >= 6) {
                    if (view != null) {
                        view.showMessage(R.string.can_not_add_more);
                    }
                } else {
                    if (view != null) {
                        view.showAddNewDevice();
                    }
                }
            }
        });
    }

    @Override
    public void appExit() {
        deviceSource.onAppExit();
    }
}
