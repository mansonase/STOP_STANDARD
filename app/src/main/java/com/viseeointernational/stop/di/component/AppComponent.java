package com.viseeointernational.stop.di.component;

import com.viseeointernational.stop.di.module.AppModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    MainActivityComponent.Builder mainActivityComponent();

    SettingActivityComponent.Builder settingActivityComponent();

    DetailActivityComponent.Builder detailActivityComponent();

    AddActivityComponent.Builder addActivityComponent();
}
