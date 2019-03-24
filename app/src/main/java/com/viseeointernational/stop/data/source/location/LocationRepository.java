package com.viseeointernational.stop.data.source.location;

import android.content.Context;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.util.List;

import javax.inject.Inject;

public class LocationRepository implements LocationSource {

    private static final String TAG = LocationRepository.class.getSimpleName();

    private LocationManager locationManager;
    private LocationClient locationClient;
    private Context context;

    @Inject
    public LocationRepository(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.context = context;
    }

    private LocationCallback locationCallback;

    @Override
    public void getLocation(@NonNull final LocationCallback callback) throws SecurityException {
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.GPS_PROVIDER) || providers.contains(LocationManager.NETWORK_PROVIDER)) {
            locationClient = new LocationClient(context);
            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setOpenGps(true);
            option.setWifiCacheTimeOut(5 * 60 * 1000);
            locationClient.setLocOption(option);
            locationClient.registerLocationListener(listener);
            locationCallback = callback;
            locationClient.start();
        } else {
            callback.onLocationNotEnable();
        }
    }

    private BDAbstractLocationListener listener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            Log.d(TAG, bdLocation.getLatitude() + "  " + bdLocation.getLongitude());
            locationClient.unRegisterLocationListener(this);
            if (locationCallback != null) {
                locationCallback.onLocationLoaded(bdLocation.getLatitude(), bdLocation.getLongitude());
                locationCallback = null;
            }
        }
    };
}
