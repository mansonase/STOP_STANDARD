package com.viseeointernational.stop.di;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.viseeointernational.stop.data.source.android.ble.BleService;
import com.viseeointernational.stop.data.source.base.database.DeviceDao;
import com.viseeointernational.stop.data.source.base.database.StateDao;
import com.viseeointernational.stop.data.source.base.database.StopDatabase;
import com.viseeointernational.stop.data.source.base.sharedpreferences.SharedPreferencesHelper;
import com.viseeointernational.stop.data.source.device.DeviceRepository;
import com.viseeointernational.stop.data.source.device.DeviceSource;
import com.viseeointernational.stop.data.source.location.LocationRepository;
import com.viseeointernational.stop.data.source.location.LocationSource;
import com.viseeointernational.stop.di.component.AddActivityComponent;
import com.viseeointernational.stop.di.component.DetailActivityComponent;
import com.viseeointernational.stop.di.component.GuideActivityComponent;
import com.viseeointernational.stop.di.component.MainActivityComponent;
import com.viseeointernational.stop.di.component.SettingActivityComponent;
import com.viseeointernational.stop.view.notification.Notifications;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(subcomponents = {MainActivityComponent.class,
        GuideActivityComponent.class,
        SettingActivityComponent.class,
        DetailActivityComponent.class,
        AddActivityComponent.class})
public class AppModule {

    private static final String DATABASE_NAME = "Stop.db";
    private static final String SHARED_PREFERENCES_NAME = "Config";

    private Context context;
    private BleService bleService;

    public AppModule(Context context, BleService bleService) {
        this.context = context;
        this.bleService = bleService;
    }

    @Singleton
    @Provides
    Context context() {
        return context;
    }

    @Singleton
    @Provides
    BleService bleService() {
        return bleService;
    }

    @Singleton
    @Provides
    StopDatabase conditionDatabase(Context context) {
        return Room.databaseBuilder(context, StopDatabase.class, DATABASE_NAME)
//                .addMigrations(StopDatabase.MIGRATION_1_2)
                .fallbackToDestructiveMigration()
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

    @Singleton
    @Provides
    Notifications notifications(Context context) {
        return new Notifications(context);
    }

    @Singleton
    @Provides
    DeviceSource deviceSource(DeviceRepository deviceRepository) {
        return deviceRepository;
    }

    @Singleton
    @Provides
    LocationSource locationSource(LocationRepository locationRepository) {
        return locationRepository;
    }
}
