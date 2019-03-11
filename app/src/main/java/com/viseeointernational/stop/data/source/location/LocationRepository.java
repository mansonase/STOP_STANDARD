package com.viseeointernational.stop.data.source.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LocationRepository implements LocationSource {

    private static final String TAG = LocationRepository.class.getSimpleName();

    private LocationManager locationManager;

    @Inject
    public LocationRepository(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    private LocationCallback locationCallback;

    @Override
    public void getLocation(@NonNull LocationCallback callback) throws SecurityException {
        List<String> providers = locationManager.getProviders(true);
        String locationProvider;
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            locationProvider = LocationManager.GPS_PROVIDER;
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            callback.onLocationNotEnable();
            return;
        }

        Location location = locationManager.getLastKnownLocation(locationProvider);
        if (location != null) {
            callback.onLocationLoaded(location.getLatitude(), location.getLongitude());
            Log.d(TAG, "直接获取位置");
            return;
        }
        locationCallback = callback;
        locationManager.requestLocationUpdates(locationProvider, 1000, 1, listener);
        startListen();
    }

    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (locationCallback != null) {
                locationCallback.onLocationLoaded(location.getLatitude(), location.getLongitude());
                locationCallback = null;
            }
            Log.d(TAG, "监听获取位置");
            locationManager.removeUpdates(this);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private Disposable disposable;

    private void startListen() {
        stopListen();
        disposable = Observable.timer(15, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        stopListen();
                        locationManager.removeUpdates(listener);
                        if (locationCallback != null) {
                            locationCallback.onTimeOut();
                            locationCallback = null;
                        }
                    }
                });
    }

    private void stopListen() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
    }
}
