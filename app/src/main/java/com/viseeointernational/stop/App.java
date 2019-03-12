package com.viseeointernational.stop;

import android.app.Application;

import com.viseeointernational.stop.data.source.android.ble.BleService;
import com.viseeointernational.stop.di.component.AppComponent;
import com.viseeointernational.stop.di.component.DaggerAppComponent;
import com.viseeointernational.stop.di.module.AppModule;

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
