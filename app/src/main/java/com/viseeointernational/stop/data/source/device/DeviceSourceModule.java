package com.viseeointernational.stop.data.source.device;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DeviceSourceModule {

    @Singleton
    @Provides
    DeviceSource deviceSource(DeviceRepository repository) {
        return repository;
    }
}
