package com.viseeointernational.stop.data.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.viseeointernational.stop.data.source.device.HistoryDataSet;
import com.viseeointernational.stop.data.source.device.OperateTimer;

@Entity
public class Device {

    @PrimaryKey
    @NonNull
    public String address;// 物理地址

    public String pairId;// 长度8 使用时转成4字节

    public String name;// 名字

    public String imagePath;// 头像

    public long time;// 配对时间

    public String timeFormat;// 日期格式

    public int alertTune;// 提醒铃声类型

    public int notificationType;// 通知类型

    public boolean enableAlert;// 是否开启蜂鸣器

    public boolean enableMonitoring;// 是否开启声音

    public boolean enableG;// 是否开启G振动

    public boolean enableXYZ;// 是否开启XYZ振动

    public byte gValue;// g灵敏度

    public byte xyzValue;// xyz灵敏度

    public int defaultShow;// hour day month year

    @Ignore
    public int connectionState;// 是否已连接

    @Ignore
    public int movementsCount;// 当前触发次数

    @Ignore
    public int battery;// 电量

    @Ignore
    public long lastUpdateTime;

    @Ignore
    public State currentState;// 当前状态

    @Ignore
    public int rssi;

    @Ignore
    public HistoryDataSet historyDataSet;

    @Ignore
    public boolean isReady;

    @Ignore
    public OperateTimer historyTimer;
}
