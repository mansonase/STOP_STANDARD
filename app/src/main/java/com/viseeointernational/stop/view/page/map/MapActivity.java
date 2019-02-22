package com.viseeointernational.stop.view.page.map;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.viseeointernational.stop.R;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";

    private double longitude;
    private double latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Intent intent = getIntent();
        if (intent != null) {
            latitude = intent.getDoubleExtra(KEY_LATITUDE, 0);
            longitude = intent.getDoubleExtra(KEY_LONGITUDE, 0);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng lng = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(lng).title("STOP"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lng, 12));
    }
}
