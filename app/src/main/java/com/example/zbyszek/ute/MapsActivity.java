package com.example.zbyszek.ute;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener {

    private GoogleApiClient mGoogleApiClient;
    private final static int MY_LOCATION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private Location mLastLocation;
    private List<APIExecutor> apiExes;

    private ListView mListView;
    private ArrayAdapter<String> mAdapter;

    private LatLng myLastLocation;
    private LatLng currentLocation;
    private BottomSheetBehavior bottomSheetBehavior;

    private boolean isConnected = false;
    private Marker myLocationMarker;
    private Marker addedMarker;
    private Circle myLocationCircle;

    private String distanceStr;
    private List<Integer> UMWWaPlaces;
    private List<Integer> GooglePlaces;

    private double distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maps_activity);
        createGoogleApiClient();
        APIExecutor.setContextAndApis(this);
        mapFragment.getMapAsync(this);

        initLegendView();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN
                        || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
    }

    private void createGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public GoogleMap getMap() {
        return mMap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        mMap.setMyLocationEnabled(true);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
//        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//            @Override
//            public boolean onMarkerClick(Marker marker) {
//                return false;
//            }
//        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions, MY_LOCATION_REQUEST_CODE);
            }
        } else
            mMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (isConnected)
            return;
        Intent intent = getIntent();
        distanceStr = intent.getStringExtra("distance");
        UMWWaPlaces = intent.getIntegerArrayListExtra("UMWwaChecked");
        GooglePlaces = intent.getIntegerArrayListExtra("GoogleChecked");
        distance = Double.valueOf(distanceStr);
        LatLng location = animateMyLocation();
        initApis(location);
        isConnected = true;
    }

    private void initApis(LatLng location) {
        apiExes = new ArrayList<>();
        for (int i = 0; i < UMWWaPlaces.size(); i++) {
            int placeId = UMWWaPlaces.get(i);
            float markerId = (i + 1) * 30f;
            APIExecutor apiExe = new APIExecutor(mMap, location, distanceStr, placeId, markerId);
            apiExes.add(apiExe);
            apiExe.execute();
            if (mAdapter.getCount() < UMWWaPlaces.size()) {
                mAdapter.add(getResources().getStringArray(R.array.places)[placeId - 100]);
                mListView.setItemChecked(mListView.getCount() - 1, true);
            } else {
                mListView.setItemChecked(i,true);
            }
        }
        for (int i = 0; i < GooglePlaces.size(); i++) {
            int placeId = GooglePlaces.get(i);
            float markerId = (UMWWaPlaces.size() + i + 1) * 30f;
            APIExecutor apiExe = new APIExecutor(mMap, location, distanceStr, placeId, markerId);
            apiExes.add(apiExe);
            apiExe.execute();
            if (mAdapter.getCount() < UMWWaPlaces.size() + GooglePlaces.size()) {
                mAdapter.add(getResources().getStringArray(R.array.google_places)[placeId]);
                mListView.setItemChecked(mListView.getCount() - 1, true);
            } else {
                mListView.setItemChecked(UMWWaPlaces.size() + i,true);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void initLegendView() {
//        mListView = new ListView(this);
        LinearLayout ll = (LinearLayout) findViewById(R.id.bottom_sheet);
        mListView = (ListView) ll.findViewById(R.id.legListView);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice) {
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                return view;
            }
        };
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListView.isItemChecked(position)) {
                    apiExes.get(position).showMarkers();
                } else {
                    apiExes.get(position).hideMarkers();
                }
            }
        });
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setAdapter(mAdapter);
        bottomSheetBehavior = BottomSheetBehavior.from(ll);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private LatLng animateMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        LatLng
        currentLocation = myLastLocation = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
        markLocation(myLastLocation);
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel));
        return myLastLocation;
    }

    private void markLocation(LatLng location) {
        myLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Tu jesteś")
                .snippet(mLastLocation.getLatitude() + ", " + mLastLocation.getLongitude())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        myLocationCircle = mMap.addCircle(new CircleOptions().center(location).radius(distance).strokeColor(Color.LTGRAY));
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(toBounds(location,distance),0));
    }

    public LatLngBounds toBounds(LatLng center, double radius) {
        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);
        return new LatLngBounds(southwest, northeast);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (marker.equals(addedMarker) || marker.equals(myLocationMarker)) {
            if (!currentLocation.equals(marker.getPosition())) {
                currentLocation = marker.getPosition();
                myLocationCircle.setCenter(marker.getPosition());
                for (APIExecutor apiExe : apiExes) {
                    apiExe.removeMarkers();
                }
                initApis(currentLocation);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(toBounds(currentLocation,distance),0));
                marker.hideInfoWindow();
//                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(toBounds(currentLocation,distance),0));
            }
        } else {
            float[] distance = new float[1];
            LatLng markerLocation = marker.getPosition();
            Location.distanceBetween(myLastLocation.latitude, myLastLocation.longitude, markerLocation.latitude, markerLocation.longitude, distance);
            new Messager(this, marker.getTitle(), marker.getSnippet(), distance[0]);
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (addedMarker != null) {
            addedMarker.remove();
        }
        addedMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Kliknij nowe miejsce centralne")
                .snippet(latLng.latitude + ", " + latLng.longitude)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
        addedMarker.showInfoWindow();
    }
}
