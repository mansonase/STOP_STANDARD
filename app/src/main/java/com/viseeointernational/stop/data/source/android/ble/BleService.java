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
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.viseeointernational.stop.App;
import com.viseeointernational.stop.R;
import com.viseeointernational.stop.util.WriteDataUtil;
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

    private Map<String, BleDevice> connectedDevice = new HashMap<>();

    private Disposable searchDisposable;

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Notification.Builder builder = new Notification.Builder(this);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = notificationManager.getNotificationChannel(Notifications.CHANNEL_ID_FOREGROUND);
                if (channel == null) {
                    channel = new NotificationChannel(Notifications.CHANNEL_ID_FOREGROUND, getText(R.string.channel_ble_service), NotificationManager.IMPORTANCE_NONE);
                    channel.enableLights(false);
                    channel.enableVibration(false);
                    channel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
                    notificationManager.createNotificationChannel(channel);
                }
                builder.setChannelId(Notifications.CHANNEL_ID_FOREGROUND);
            }
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle(getText(R.string.app_name));
            builder.setContentText(getText(R.string.running));
            builder.setLights(Color.GREEN, 0, 0);
            builder.setVibrate(null);
            builder.setSound(null);
            builder.setAutoCancel(true);
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
            startForeground(Notifications.NOTIFICATION_ID_FOREGROUND, builder.build());
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(registerReceiver, filter);

        ((App) getApplication()).setBleService(this);
    }

    private BroadcastReceiver registerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    for (Map.Entry<String, BleDevice> entry : connectedDevice.entrySet()) {
                        entry.getValue().disconnect();
                        entry.getValue().release();
                    }
                    connectedDevice.clear();
                    sendEvent(BleEvent.ADAPTER_DISABLE, "", "", 0, null);
                    break;
                case BluetoothAdapter.STATE_ON:
                    sendEvent(BleEvent.ADAPTER_ENABLE, "", "", 0, null);
                    break;
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(registerReceiver);
        for (Map.Entry<String, BleDevice> entry : connectedDevice.entrySet()) {
            entry.getValue().disconnect();
            entry.getValue().release();
        }
        stopForeground(true);
        super.onDestroy();
    }

    public boolean isBleAvailable() {
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.isEnabled();
        }
        return false;
    }

    public void search(int seconds) {
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            Observable.just(adapter)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new Consumer<BluetoothAdapter>() {
                        @Override
                        public void accept(BluetoothAdapter bluetoothAdapter) throws Exception {
                            bluetoothAdapter.startLeScan(leScanCallback);
                        }
                    });
            searchDisposable = Observable.timer(seconds, TimeUnit.SECONDS)
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            sendEvent(BleEvent.SEARCH_FINISH, null, null, 0, null);
                            stopSearch();
                        }
                    });
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            sendEvent(BleEvent.DEVICE_FOUND_CALLBACK, device.getAddress(), device.getName(), rssi, null);
        }
    };

    public void stopSearch() {
        if (searchDisposable != null && !searchDisposable.isDisposed()) {
            searchDisposable.dispose();
        }
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.stopLeScan(leScanCallback);
        }
    }

    public void connect(String address) {
        if (connectedDevice.containsKey(address)) {
            return;
        }
        BluetoothAdapter adapter = getAdapter();
        if (adapter != null) {
            Log.d(TAG, "正在连接 " + address);
            BluetoothDevice device = adapter.getRemoteDevice(address);
            device.connectGatt(this, false, bluetoothGattCallback);
        }
    }

    public void disconnect(String address) {
        if (connectedDevice.containsKey(address)) {
            connectedDevice.get(address).disconnect();
        }
    }

    public void write(String address, byte[] validData) {
        byte[] data = WriteDataUtil.getWriteData(validData);// 将有效数据加上起始位 长度 校验
        if (connectedDevice.containsKey(address)) {
            connectedDevice.get(address).receiveWriteData(data);
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
                    disconnected(address);
                    gatt.close();
                }
            } else {
                String address = gatt.getDevice().getAddress();
                if (!connectedDevice.containsKey(address)) {
                    gatt.close();
                    connect(address);
                } else {
                    disconnected(address);
                    gatt.close();
                }
            }
        }

        private void disconnected(String address) {
            if (connectedDevice.containsKey(address)) {
                connectedDevice.get(address).release();
                connectedDevice.remove(address);
            }
            sendEvent(BleEvent.DISCONNECTED, address, null, 0, null);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID_SERVICE);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);

                        // 连接成功
                        String address = gatt.getDevice().getAddress();
                        BleDevice tool = new BleDevice(address, gatt, characteristic);
                        connectedDevice.put(address, tool);
                        sendEvent(BleEvent.CONNECTED, address, null, 0, null);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            String address = gatt.getDevice().getAddress();

            if (connectedDevice.containsKey(address)) {
                connectedDevice.get(address).receiveReadData(data);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

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