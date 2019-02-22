package com.viseeointernational.stop.data.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class State {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int type;// 动作

    public String address;// 物理地址

    public int movementsCount;// 触发次数

    // 硬件每笔数据的索引 用来查历史记录
    public byte indexH;

    public byte indexL;

    public byte reportId;// 判断收到a0 c0 c1是否为同一组数据

    public long time;// 发生时间

    public double longitude;

    public double latitude;
}
