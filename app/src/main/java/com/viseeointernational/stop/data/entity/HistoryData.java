package com.viseeointernational.stop.data.entity;

import java.util.Calendar;

public class HistoryData {

    public HistoryData(byte indexH, byte indexL, long lastTime) {
        this.indexH = indexH;
        this.indexL = indexL;
        this.lastTime = lastTime;
        long now = Calendar.getInstance().getTimeInMillis();
        readA1Count = (int) ((now - lastTime) / (60000 * 240)) + 1;// 一次能读取4小时数据
        timeCursor = now - 60000;// 第一个数据时前一分钟的
    }

    public byte indexH;
    public byte indexL;
    public long lastTime;
    public int readA1Count;
    public long timeCursor;
}
