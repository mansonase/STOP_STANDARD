package com.viseeointernational.stop.data.source.base;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.viseeointernational.stop.data.source.base.database.DeviceDao;
import com.viseeointernational.stop.data.source.base.database.StateDao;
import com.viseeointernational.stop.data.source.base.database.StopDatabase;
import com.viseeointernational.stop.data.source.base.sharedpreferences.SharedPreferencesHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class BaseSourceModule {

    private static final String DATABASE_NAME = "Stop.db";
    private static final String SHARED_PREFERENCES_NAME = "Config";

    @Singleton
    @Provides
    StopDatabase conditionDatabase(Context context) {
        return Room.databaseBuilder(context, StopDatabase.class, DATABASE_NAME)
                .build();
    }

    @Singleton
    @Provides
    DeviceDao deviceDao(StopDatabase database) {
        return database.deviceDao();
    }

    @Singleton
    @Provides
    StateDao stateDao(StopDatabase database) {
        return database.stateDao();
    }

    @Singleton
    @Provides
    SharedPreferencesHelper sharedPreferencesHelper(Context context) {
        return new SharedPreferencesHelper(context, SHARED_PREFERENCES_NAME);
    }
}
