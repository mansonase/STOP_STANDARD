package com.viseeointernational.stop.data.source.android.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.viseeointernational.stop.util.StringUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BleDevice implements ReadThread.Callback, WriteThread.Callback {

    private static final String TAG = BleDevice.class.getSimpleName();

    private String address;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristic;

    private ReadThread readThread;
    private WriteThread writeThread;

    private volatile Semaphore semaphore = new Semaphore(1);// 创建一个信号量 使读写同步（按序读写)
    private Disposable disposable;// 3秒自动释放信号量

    public BleDevice(String address, BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
        this.address = address;
        this.bluetoothGatt = bluetoothGatt;
        this.characteristic = characteristic;
        readThread = new ReadThread(this);
        readThread.start();
        writeThread = new WriteThread(this);
        writeThread.start();
    }

    @Override
    public void onRead(byte[] data) {
        Log.d(TAG, "address = " + address + "  read = " + StringUtil.bytes2HexString(data));
        if (semaphore.availablePermits() == 0) {
            semaphore.release();
        }
        handleReadData(address, data);
    }

    @Override
    public void onWrite(byte[] data) {
        try {
            semaphore.acquire();
            startAutoReleaseSemaphore();
            characteristic.setValue(data);
            boolean state = bluetoothGatt.writeCharacteristic(characteristic);
            Log.d(TAG, "address = " + address + " write = " + StringUtil.bytes2HexString(data) + " 写" + (state ? "成功" : "失败"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        writeThread.close();
        writeThread = null;
        stopAutoReleaseSemaphore();
        readThread.close();
        readThread = null;
        semaphore.release();
        semaphore = null;
        characteristic = null;
        bluetoothGatt = null;
    }

    public void receiveReadData(byte[] data) {
        readThread.addData(data);
    }

    public void receiveWriteData(byte[] data) {
        writeThread.addData(data);
    }

    public void disconnect() {
        bluetoothGatt.disconnect();
    }

    private void stopAutoReleaseSemaphore() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
    }

    private void startAutoReleaseSemaphore() {
        stopAutoReleaseSemaphore();
        disposable = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        stopAutoReleaseSemaphore();
                        try {
                            if (semaphore.availablePermits() == 0) {
                                semaphore.release();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void handleReadData(String address, byte[] validData) {
        if (validData.length == 5 && validData[0] == (byte) 0xb0) {
            sendEvent(BleEvent.PAIR_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 2 && validData[0] == (byte) 0xb1) {
            sendEvent(BleEvent.COMFIRM_PAIR_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 12 && validData[0] == (byte) 0xb2) {
            sendEvent(BleEvent.CONNECT_SUCCESSFUL_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 2 && validData[0] == (byte) 0xee) {
            sendEvent(BleEvent.CONNECT_FAILED_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 12 && validData[0] == (byte) 0xa0) {
            sendEvent(BleEvent.A0_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 124 && validData[0] == (byte) 0xa1) {
            sendEvent(BleEvent.A1_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 2 && validData[0] == (byte) 0xa3) {
            sendEvent(BleEvent.BATTERY_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 12 && validData[0] == (byte) 0xc0) {
            sendEvent(BleEvent.C0_CALLBACK, address, null, 0, validData);
            return;
        }
        if (validData.length == 6 && validData[0] == (byte) 0xa4) {
            sendEvent(BleEvent.SET_MODE_CALLBACK, address, null, 0, validData);
        }
        if (validData.length == 1 && validData[0] == (byte) 0xaa) {
            sendEvent(BleEvent.BEEP_CALLBACK, address, null, 0, validData);
        }
        if (validData.length == 1 && validData[0] == (byte) 0xa8) {
            sendEvent(BleEvent.UNPAIR_CALLBACK, address, null, 0, validData);
        }
    }

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
