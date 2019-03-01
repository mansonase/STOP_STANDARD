package com.viseeointernational.stop.data.source.device;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.State;

import java.util.List;

public interface DeviceSource {

    boolean isBleEnable();

    void onAppExit();

    /**********************************************添加获取设备***************************************************/
    void saveDevice(@NonNull Device device);

    interface GetDeviceCallback {

        void onDeviceLoaded(Device device);

        void onDeviceNotAvailable();
    }

    void getDevice(@NonNull String address, @NonNull GetDeviceCallback callback);

    interface GetPairedDevicesCallback {

        void onDevicesLoaded(List<Device> devices);
    }

    void getPairedDevices(@NonNull GetPairedDevicesCallback callback);

    interface GetPairedDeviceCountCallback {

        void onCountLoaded(int count);
    }

    void getPairedDeviceCount(@NonNull GetPairedDeviceCountCallback callback);

    /**********************************************获取状态***************************************************/
    interface GetStatesContainTimeFormatCallback {

        void onStatesLoaded(List<State> states, String timeFormat);
    }

    void getStatesContainTimeFormat(@NonNull String address, long from, long to, @NonNull GetStatesContainTimeFormatCallback callback);

    interface GetStatesDescCallback {

        void onStatesLoaded(List<State> states);
    }

    void getStatesDesc(@NonNull String address, long from, long to, @NonNull GetStatesDescCallback callback);

    interface GetResetStatesDescCallback {

        void onStatesLoaded(List<State> states);

    }

    void getResetStatesDesc(@NonNull String address, long from, long to, @NonNull GetResetStatesDescCallback callback);

    /**********************************************监听功能***************************************************/
    interface BatteryListener {

        void onPowerReceived(int power);
    }

    void setBatteryListener(@NonNull String address, @Nullable BatteryListener listener);

    interface MovementListener {

        void onMovementReceived(@NonNull State state, @NonNull String timeFormat);
    }

    void setMovementListener(@NonNull String address, @Nullable MovementListener listener);

    interface MovementCountChangeListener {

        void onMovementCountChange(@NonNull String address, int count);
    }

    void setMovementCountChangeListener(@Nullable MovementCountChangeListener listener);

    interface DeviceConnectionChangeListener {

        void onConnectionChange(@NonNull String address, boolean isConnected);
    }

    void setDeviceConnectionChangeListener(@Nullable DeviceConnectionChangeListener listener);

    interface DeviceCountChangeListener {

        void onDeviceCountChange(@NonNull List<Device> list);
    }

    void setDeviceCountChangeListener(@Nullable DeviceCountChangeListener listener);

    /**********************************************不需要ble回调的功能***************************************************/
    void enableMonitoring(@NonNull String address, boolean enable);

    void setName(@NonNull String address, @NonNull String name);

    void setHeader(@NonNull String address, @NonNull Bitmap bitmap);

    void setTimeFormat(@NonNull String address, @NonNull String format);

    void setAlertTune(@NonNull String address, int alertTune);

    void setNotification(@NonNull String address, int notificationType);

    interface ResetCallback {

        void onSuccessful();

        void onAlreadyReset();

        void onBleNotAvailable();

        void onDeviceDisconnected();

        void onDeviceNotAvailable();
    }

    void reset(@NonNull String address, double latitude, double longitude, @NonNull ResetCallback callback);

    /**********************************************需要ble回调的功能***************************************************/
    interface SearchCallback {

        void onBleNotAvailable();

        void onDeviceFound(Device device);

        void onFinish();
    }

    void search(int seconds, @NonNull SearchCallback callback);

    void stopSearch();


    interface ConnectionCallback {

        void onBleNotAvailable();

        void onConnected();

        void onDisconnected();
    }

    void connect(@NonNull String address, @Nullable ConnectionCallback callback);


    interface SettingCallback {

        void onSuccessful();

        void onFailed();

        void onTimeOut();

        void onBleNotAvailable();

        void onDeviceDisconnected();

        void onDeviceNotAvailable();
    }

    void find(@NonNull String address, @NonNull SettingCallback callback);

    void enableAlert(@NonNull String address, boolean enable, @NonNull SettingCallback callback);

    void unpair(@NonNull String address, boolean force, @NonNull SettingCallback callback);

    void enableGAndXYZ(@NonNull String address, boolean enableG, boolean enableXYZ, @NonNull SettingCallback callback);

    void setG(@NonNull String address, byte g, @NonNull SettingCallback callback);

    void setXYZ(@NonNull String address, byte xyz, @NonNull SettingCallback callback);
}
