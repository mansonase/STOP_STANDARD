package com.viseeointernational.stop.data.source.android.ble;

import java.util.LinkedList;
import java.util.List;

public class WriteData {

    private List<byte[]> list = new LinkedList<>();

    public void clear() {
        synchronized (this) {
            list.clear();
        }
    }

    public void add(byte[] data) {
        synchronized (this) {
            list.add(data);
        }
    }

    public void remove(int index) {
        synchronized (this) {
            if (list.size() > index) {
                list.remove(index);
            }
        }
    }

    public byte[] get(int index) {
        synchronized (this) {
            if (list.size() > index) {
                return list.get(index);
            }
            return null;
        }
    }

    public int size() {
        synchronized (this) {
            return list.size();
        }
    }
}
