package com.viseeointernational.stop.data.source.android.ble;

public class BleEvent {

    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 2;
    public static final int PAIR_CALLBACK = 3;
    public static final int COMFIRM_PAIR_CALLBACK = 4;
    public static final int CONNECT_SUCCESSFUL_CALLBACK = 5;
    public static final int CONNECT_FAILED_CALLBACK = 6;
    public static final int A0_CALLBACK = 7;
    public static final int A1_CALLBACK = 8;
    public static final int BATTERY_CALLBACK = 9;
    public static final int C0_CALLBACK = 10;
    public static final int SET_MODE_CALLBACK = 11;
    public static final int DEVICE_FOUND_CALLBACK = 12;
    public static final int SEARCH_FINISH = 13;
    public static final int BEEP_CALLBACK = 14;
    public static final int UNPAIR_CALLBACK = 15;

    public static final int ADAPTER_ENABLE = 16;
    public static final int ADAPTER_DISABLE = 17;

    public String address;
    public String name;
    public int rssi;
    public int type;
    public byte[] value;
}
