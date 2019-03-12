package com.viseeointernational.stop.data.source.android.ble;

import android.support.annotation.NonNull;

public class WriteThread extends Thread {

    private Callback callback;

    private WriteData writeData = new WriteData();

    private boolean isStop;

    public WriteThread(@NonNull Callback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        while (!isStop && !isInterrupted()) {
            if (writeData.size() > 0) {
                byte[] data = writeData.get(0);
                writeData.remove(0);
                callback.onWrite(data);
            } else {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addData(byte[] data) {
        writeData.add(data);
    }

    public void close() {
        isStop = true;
        interrupt();
        writeData.clear();
    }

    public interface Callback {

        void onWrite(byte[] data);
    }
}
