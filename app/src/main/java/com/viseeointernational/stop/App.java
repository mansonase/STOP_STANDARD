package com.viseeointernational.stop;

import android.app.Application;

import com.viseeointernational.stop.data.source.android.ble.BleService;
import com.viseeointernational.stop.di.AppComponent;
import com.viseeointernational.stop.di.AppModule;
import com.viseeointernational.stop.di.DaggerAppComponent;

public class App extends Application {

    private BleService bleService;
    private AppComponent appComponent;

    public void setBleService(BleService bleService) {
        this.bleService = bleService;
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(getApplicationContext(), bleService)).build();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}
