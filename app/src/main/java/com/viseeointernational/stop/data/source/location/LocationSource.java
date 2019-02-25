package com.viseeointernational.stop.data.source.location;

import android.support.annotation.NonNull;

public interface LocationSource {

    void getLocation(@NonNull LocationCallback callback);

    interface LocationCallback{

        void onLocationNotEnable();

        void onLocationLoaded(double latitude, double longitude);

        void onTimeOut();
    }
}
