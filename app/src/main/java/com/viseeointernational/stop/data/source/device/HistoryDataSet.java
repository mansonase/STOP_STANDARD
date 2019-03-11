package com.viseeointernational.stop.data.source.device;

import android.util.Log;

import com.viseeointernational.stop.data.constant.StateType;
import com.viseeointernational.stop.data.entity.State;

import java.util.LinkedList;
import java.util.List;

public class HistoryDataSet {

    private static final String TAG = HistoryDataSet.class.getSimpleName();

    private String address;
    private byte indexH;
    public byte indexL;
    private long startTime;

    private byte pageIndex;

    private long timeCursor;

    private List<State> receivedData = new LinkedList<>();
    private int totalCount;

    private byte[] a1 = new byte[5];

    public HistoryDataSet(String address, byte indexH, byte indexL, long startTime, long endTime) {
        this.address = address;
        this.indexH = indexH;
        this.indexL = indexL;
        this.startTime = startTime;
        timeCursor = endTime - 1000 * 60;// 第一个数据是endTime前一分钟的
    }

    // 时间推前1分钟
    public void next() {
        timeCursor -= 1000 * 60;
    }

    public void putState(int count) {
        if (count > 0) {
            State state = new State();
            state.address = address;
            state.type = StateType.HISTORY;
            state.time = timeCursor;
            state.movementsCount = count;
            receivedData.add(state);
            totalCount += count;
        }
    }

    public boolean isEnd() {
        return timeCursor <= startTime;
    }

    public byte[] createA1Cmd() {
        a1[0] = (byte) 0xa1;
        a1[1] = pageIndex;
        a1[2] = indexH;
        a1[3] = indexL;
        a1[4] = (byte) 0x02;
        Log.d(TAG, "创建A1指令 pageIndex = " + (pageIndex & 0xff) + " indexH = " + (indexH & 0xff) + " indexL = " + (indexL & 0xff));
        pageIndex++;
        return a1;
    }

    public List<State> getReceivedData() {
        return receivedData;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public byte[] getA1() {
        return a1;
    }
}
