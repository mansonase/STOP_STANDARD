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

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    @Override
    public void onRead(byte[] data) {
        Log.d(TAG, "address = " + address + "  read = " + StringUtil.bytes2HexString(data));
        if (semaphore.availablePermits() == 0) {
            semaphore.release();
        }
        BleEvent event = new BleEvent();
        event.type = BleEvent.READ_DATA;
        event.address = address;
        event.value = data;
        EventBus.getDefault().post(event);
    }

    @Override
    public void onWrite(byte[] data) {
        try {
            semaphore.acquire();
            startAutoReleaseSemaphore();
            characteristic.setValue(data);
            boolean state = bluetoothGatt.writeCharacteristic(characteristic);
            Log.d(TAG, "address = " + address + " write = "
                    + StringUtil.bytes2HexString(data) + " 写" + (state ? "成功" : "失败"));
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

    public void receiveWriteData(byte[] validData) {
        byte[] data = getWriteData(validData);
        writeThread.addData(data);
    }

    /**
     * 获取完整数据
     *
     * @param validData 除了开头 长度 校验之后的有效数据
     * @return
     */
    private byte[] getWriteData(byte[] validData) {
        byte[] ret = new byte[validData.length + 4];
        ret[0] = (byte) 0xff;
        ret[1] = (byte) 0xaa;
        ret[2] = (byte) (validData.length + 1);
        ret[ret.length - 1] = ret[2];
        for (byte b : validData) {
            ret[ret.length - 1] += b;
        }
        System.arraycopy(validData, 0, ret, 3, validData.length);
        return ret;
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

}
