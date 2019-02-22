package com.viseeointernational.stop.data.source.base.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.State;

@Database(entities = {Device.class, State.class}, version = 1, exportSchema = false)
public abstract class StopDatabase extends RoomDatabase {

    public abstract DeviceDao deviceDao();

    public abstract StateDao stateDao();
}
