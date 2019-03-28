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
        if (!providers.contains(LocationManager.GPS_PROVIDER) && !providers.contains(LocationManager.NETWORK_PROVIDER)) {
            callback.onLocationNotEnable();
            return;
        }
        locationCallback = callback;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
        startListen();
    }

    private void getLocation(Location location) {
        if (locationCallback != null) {
            locationCallback.onLocationLoaded(location.getLatitude(), location.getLongitude());
            locationCallback = null;
        }
        locationManager.removeUpdates(gpsListener);
        locationManager.removeUpdates(networkListener);
    }

    private LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "gps获取位置");
            getLocation(location);
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

    private LocationListener networkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "网络获取位置");
            getLocation(location);
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
        disposable = Observable.timer(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        stopListen();
                        locationManager.removeUpdates(gpsListener);
                        locationManager.removeUpdates(networkListener);
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
