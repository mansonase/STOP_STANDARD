package com.viseeointernational.stop.data.source.android.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.viseeointernational.stop.App;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.view.notification.Notifications;
import com.viseeointernational.stop.view.page.main.MainActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BleService extends Service {

    private static final String TAG = BleService.class.getSimpleName();

    private static final UUID UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123");
    private static final UUID UUID_CHARACTERISTIC = UUID.fromString("00001525-1212-efde-1523-785feabcd123");
    private static final UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Map<String, BleDevice> connectedDevice = new HashMap<>();// 保存已连接的设备

    private Disposable disposable;// 搜索

    private BroadcastReceiver registerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:// 蓝牙关闭时
                    Log.d(TAG, "蓝牙关闭");
                    clearDevices();
                    sendEvent(BleEvent.BLUETOOTH_ADAPTER_DISABLE, null, null, 0, null);
                    break;
                case BluetoothAdapter.STATE_ON:// 蓝牙开启时
                    Log.d(TAG, "蓝牙开启");
                    sendEvent(BleEvent.BLUETOOTH_ADAPTER_ENABLE, null, null, 0, null);
                    break;
            }
        }
    };

    private int reconnectCount;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "服务创建");

        startForeground();// 开启前台服务

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(registerReceiver, filter);

        ((App) getApplication()).setBleService(this);// 依赖注入
    }

    private void startForeground() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        Notification.Builder builder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = notificationManager.getNotificationChannel(Notifications.CHANNEL_ID_FOREGROUND);
            if (channel == null) {
                channel = new NotificationChannel(Notifications.CHANNEL_ID_FOREGROUND, getText(R.string.channel_ble_service), NotificationManager.IMPORTANCE_NONE);
                notificationManager.createNotificationChannel(channel);
            }
            builder.setChannelId(Notifications.CHANNEL_ID_FOREGROUND);
        }
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(getText(R.string.app_name));
        builder.setContentText(getText(R.string.running));
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        startForeground(Notifications.NOTIFICATION_ID_FOREGROUND, builder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "服务销毁");
        unregisterReceiver(registerReceiver);
        clearDevices();
        stopForeground(true);
        super.onDestroy();
    }

    // 清除已连接设备
    private void clearDevices() {
        for (Map.Entry<String, BleDevice> entry : connectedDevice.entrySet()) {
            BleDevice bleDevice = entry.getValue();
            BluetoothGatt gatt = bleDevice.getBluetoothGatt();
            gatt.disconnect();
            gatt.close();
            bleDevice.release();
        }
        connectedDevice.clear();
    }

    // 蓝牙是否开启
    public boolean isBleAvailable() {
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.isEnabled();
        }
        return false;
    }

    // 搜索
    public void search(int seconds) {
        final BluetoothAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
        Observable.just(1)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        adapter.stopLeScan(leScanCallback);
                        adapter.startLeScan(leScanCallback);
                    }
                });
        disposable = Observable.timer(seconds, TimeUnit.SECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        stopSearch();
                        sendEvent(BleEvent.SEARCH_FINISH, null, null, 0, null);
                    }
                });

    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            sendEvent(BleEvent.SEARCH_DEVICE_FOUND, device.getAddress(), device.getName(), rssi, null);
        }
    };

    public void stopSearch() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
        final BluetoothAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        Observable.just(1)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        adapter.stopLeScan(leScanCallback);
                    }
                });
    }

    public void connect(String address, boolean reconnect) {
        if (connectedDevice.containsKey(address)) {
            return;
        }
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            if(reconnect){
                reconnectCount = 3;
            }
            Log.d(TAG, "正在连接 " + address);
            BluetoothDevice device = adapter.getRemoteDevice(address);
            device.connectGatt(this, false, bluetoothGattCallback);
        }
    }

    public void disconnect(String address) {
        if (connectedDevice.containsKey(address)) {
            connectedDevice.get(address).getBluetoothGatt().disconnect();
        }
    }

    public void write(String address, byte[] validData) {
        if (connectedDevice.containsKey(address)) {
            connectedDevice.get(address).receiveWriteData(validData);
        }
    }

    private BluetoothAdapter getAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            return bluetoothManager.getAdapter();
        }
        return null;
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                } else {
                    String address = gatt.getDevice().getAddress();
                    gatt.close();
                    Log.d(TAG, "已断开连接 " + address);
                    releaseDevice(address);
                    sendEvent(BleEvent.GATT_DISCONNECTED, address, null, 0, null);
                }
            } else {
                String address = gatt.getDevice().getAddress();
                gatt.close();
                if (!connectedDevice.containsKey(address)) {// 正在连接的设备要重连
                    if (reconnectCount > 0) {
                        reconnectCount--;
                        Log.d(TAG, "正在重连 " + address);
                        connect(address, false);
                    } else {
                        Log.d(TAG, "连接失败 " + address);
                        releaseDevice(address);
                        sendEvent(BleEvent.GATT_DISCONNECTED, address, null, 0, null);
                    }
                } else {// 已连接的设备断开连接
                    Log.d(TAG, "连接失败 " + address);
                    releaseDevice(address);
                    sendEvent(BleEvent.GATT_DISCONNECTED, address, null, 0, null);
                }
            }
        }

        // 释放设备
        private void releaseDevice(String address) {
            if (connectedDevice.containsKey(address)) {
                connectedDevice.get(address).release();
                connectedDevice.remove(address);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID_SERVICE);
                if (service == null) {
                    return;
                }
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC);
                if (characteristic == null) {
                    return;
                }
                gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);

                // 连接成功
                String address = gatt.getDevice().getAddress();
                BleDevice tool = new BleDevice(address, gatt, characteristic);
                connectedDevice.put(address, tool);
                Log.d(TAG, "连接成功 " + address);
                sendEvent(BleEvent.GATT_CONNECTED, address, null, 0, null);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            String address = gatt.getDevice().getAddress();
            if (connectedDevice.containsKey(address)) {// 收到数据丢给相应的设备
                connectedDevice.get(address).receiveReadData(data);
            }
        }
    };

    private void sendEvent(int type, String address, String name, int rssi, byte[] value) {
        BleEvent event = new BleEvent();
        event.type = type;
        event.address = address;
        event.name = name;
        event.rssi = rssi;
        event.value = value;
        EventBus.getDefault().post(event);
    }

}