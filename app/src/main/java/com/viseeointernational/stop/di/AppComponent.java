package com.viseeointernational.stop.di;

import com.viseeointernational.stop.di.component.AddActivityComponent;
import com.viseeointernational.stop.di.component.DetailActivityComponent;
import com.viseeointernational.stop.di.component.GuideActivityComponent;
import com.viseeointernational.stop.di.component.MainActivityComponent;
import com.viseeointernational.stop.di.component.SettingActivityComponent;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    MainActivityComponent.Builder mainActivityComponent();

    SettingActivityComponent.Builder settingActivityComponent();

    DetailActivityComponent.Builder detailActivityComponent();

    AddActivityComponent.Builder addActivityComponent();

    GuideActivityComponent.Builder guideActivityComponent();
}
