package com.viseeointernational.stop.data.source.android.ble;

import android.support.annotation.NonNull;

public class ReadThread extends Thread {

    private ReadData readData = new ReadData();

    private Callback callback;

    private boolean isStop;

    private boolean isFF;
    private boolean isAA;
    private byte[] result;// 不包含长度位和校验位
    private int index;
    private byte chksum;

    public ReadThread(@NonNull Callback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        while (!isStop && !isInterrupted()) {
            if (readData.size() > 0) {
                byte b = readData.get(0);
                readData.remove(0);
                if (!isFF) {
                    if (b == (byte) 0xff) {
                        isFF = true;// 判断第一个字节是ff
                    }
                } else {
                    if (!isAA) {
                        if (b == (byte) 0xaa) {
                            isAA = true;// 判断第二个字节是aa
                            index = 3;
                            result = null;
                            chksum = (byte) 0x00;
                        } else {
                            isFF = false;
                        }
                    } else {
                        if (result == null) {
                            result = new byte[b + 3];// 第三个字节是长度
                            result[0] = (byte) 0xff;
                            result[1] = (byte) 0xaa;
                            result[2] = b;
                            chksum += b;
                        } else {
                            if (index == result.length - 1) {// 判断读到最后一条数据的最后一个字节
                                if (chksum == b) {// 判断校验是否正确
                                    result[index] = chksum;
                                    callback.onRead(result);
                                }
                                isFF = isAA = false;
                            } else {
                                result[index] = b;
                                chksum += b;
                            }
                            index++;
                        }
                    }
                }
            } else {
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addData(byte[] data) {
        readData.add(data);
    }

    public void close() {
        isStop = true;
        interrupt();
    }

    public interface Callback {

        void onRead(byte[] data);
    }
}
