package com.viseeointernational.stop.data.source.base.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;

import com.viseeointernational.stop.data.entity.Device;
import com.viseeointernational.stop.data.entity.State;

@Database(entities = {Device.class, State.class}, version = 2, exportSchema = false)
public abstract class StopDatabase extends RoomDatabase {

//    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
//        @Override
//        public void migrate(SupportSQLiteDatabase database) {
//            database.execSQL("ALTER TABLE Device  ADD COLUMN defaultShow INTEGER");
//        }
//    };

    public abstract DeviceDao deviceDao();

    public abstract StateDao stateDao();
}
