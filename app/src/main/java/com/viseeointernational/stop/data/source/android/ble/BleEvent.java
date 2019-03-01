package com.viseeointernational.stop.data.source.android.ble;

public class BleEvent {

    public static final int GATT_CONNECTED = 1;
    public static final int GATT_DISCONNECTED = 2;
    public static final int SEARCH_DEVICE_FOUND = 3;
    public static final int SEARCH_FINISH = 4;
    public static final int BLUETOOTH_ADAPTER_ENABLE = 5;
    public static final int BLUETOOTH_ADAPTER_DISABLE = 6;
    public static final int READ_DATA = 7;

    public String address;
    public String name;
    public int rssi;
    public int type;
    public byte[] value;
}
