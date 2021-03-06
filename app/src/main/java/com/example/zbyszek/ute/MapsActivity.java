package com.example.zbyszek.ute;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener,
        LocationListener {

    private static final int PLACE_PICKER_REQUEST_CODE = 0;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "Location Updates Requesting Key";
    private static final String LOCATION_KEY = "Location Key";

    private static final float MY_LOCATION_HUE = BitmapDescriptorFactory.HUE_RED;
    private static final float ADDED_MARKER_HUE = BitmapDescriptorFactory.HUE_ROSE;

    private Messager messager;

    private boolean mRequestingLocationUpdates = true;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private Location mLastLocation;
    private List<APIExecutor> apiExes;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private LatLng myLastLocation;
    private LatLng centralLocation;
    private BottomSheetBehavior bottomSheetBehavior;

    private Marker myLocationMarker;
    private Marker addedMarker;
    private Marker centralMarker;

    private Circle currentLocationCircle;
    private String distanceString;
    private double distance;
    private LocationRequest mLocationRequest;
    private Geocoder geocoder;
    private EditText distanceEditText;

    private Map<String, Messager> messagerMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);
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

        updateValuesFromBundle(savedInstanceState);
    }

    private void createGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnInfoWindowLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Context mContext = getApplicationContext();
                LinearLayout infoWindow = new LinearLayout(mContext);
                infoWindow.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(mContext);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    title.setTextColor(getColor(R.color.colorPrimary));
                } else {
                    title.setTextColor(Color.BLACK);
                }
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(mContext);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    snippet.setTextColor(getColor(R.color.colorPrimaryDark));
                } else {
                    snippet.setTextColor(Color.GRAY);
                }
                snippet.setGravity(Gravity.CENTER);
                snippet.append(marker.getSnippet());
                ;

                infoWindow.addView(title);
                infoWindow.addView(snippet);

                if (!marker.equals(centralMarker)) {
                    LatLng markerLocation = marker.getPosition();
                    float[] distance = new float[1];
                    Location.distanceBetween(centralLocation.latitude, centralLocation.longitude, markerLocation.latitude, markerLocation.longitude, distance);
                    if (distance[0] > 0 && !marker.equals(myLocationMarker)) {
                        TextView tv = new TextView(mContext);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            tv.setTextColor(getColor(R.color.colorPrimaryDark));
                        }
                        tv.setGravity(Gravity.CENTER);
                        tv.setText("W linii prostej: " + Math.round(distance[0]) + " m");
                        infoWindow.addView(tv);
                    }
                }
                return infoWindow;
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        } else {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    animateMyLocation();
                    return false;
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        outState.putParcelable(LOCATION_KEY, mLastLocation);

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (savedInstanceState.containsKey((LOCATION_KEY))) {
                mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
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
        if (mRequestingLocationUpdates) {
            mLocationRequest = LocationRequest.create().setSmallestDisplacement(100);
            startLocationUpdates();
        }
        init();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        } else
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void initApis(List<Integer> checkedPlaces, int apiSeparator, String distance, LatLng location) {
        if (!checkInternet())
            return;
        apiExes = new ArrayList<>();
        messagerMap = new HashMap<>();
        for (int i = 0; i < checkedPlaces.size(); i++) {
            int placeId = checkedPlaces.get(i);
            float markerId = (i + 1) * 30f;
            APIExecutor apiExe = new APIExecutor(mMap, location, distance, placeId, markerId);
            apiExes.add(apiExe);
            apiExe.execute();
            String place = null;
            if (i < apiSeparator) {
                place = getResources().getStringArray(R.array.places)[placeId - 100];
            } else {
                place = getResources().getStringArray(R.array.google_places)[placeId];
            }
            mAdapter.add(place);
            mListView.setItemChecked(mListView.getCount() - 1, true);
        }
    }

    private void updateApis() {
        messagerMap = new HashMap<>();
        for (int i = 0; i < apiExes.size(); i++) {
            apiExes.get(i).update(centralLocation, distanceString);
            mListView.setItemChecked(i, true);
        }
        currentLocationCircle.setCenter(centralLocation);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(toBounds(centralLocation, distance), 0));
    }

    private void initLegendView() {
        LinearLayout ll = (LinearLayout) findViewById(R.id.bottom_sheet);
        mListView = (ListView) ll.findViewById(R.id.legListView);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice) {
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
                float[] hsv = new float[3];
                hsv[0] = (position + 1) * 30f;
                hsv[1] = 0.9f;
                hsv[2] = 0.7f;
                view.setBackgroundColor(ColorUtils.HSLToColor(hsv));
                view.setTextColor(Color.BLACK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    view.setCheckMarkTintList(ColorStateList.valueOf(Color.WHITE));
                }
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

    private boolean animateMyLocation() {
        LatLng location = null;
        String title = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            location = new LatLng(52.2191042, 21.011607000000026);
            title = getString(R.string.EiTI);
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation == null)
                return false;
            location = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            title = getString(R.string.my_location);
        }
        centralLocation = location;
        if (currentLocationCircle == null)
            currentLocationCircle = mMap.addCircle(new CircleOptions().center(centralLocation).radius(distance).strokeColor(Color.LTGRAY));
        else
            currentLocationCircle.setCenter(centralLocation);
        centralMarker = markMyLocation(centralLocation, title);
        return true;
    }

    private Marker markMyLocation(LatLng location, String title) {
        myLastLocation = location;
        myLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(title)
                .snippet(writeAddressAndLocation(location))
                .icon(BitmapDescriptorFactory.defaultMarker(MY_LOCATION_HUE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(toBounds(location, distance), 0));
        return myLocationMarker;
    }

    private void markMyLocation(LatLng location) {
        markMyLocation(location, getString(R.string.my_location));
    }

    private LatLngBounds toBounds(LatLng center, double radius) {
        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);
        return new LatLngBounds(southwest, northeast);
    }

    private String writeAddressAndLocation(LatLng location) {
        if (geocoder == null)
            geocoder = new Geocoder(this, Locale.getDefault());
        if (checkInternet())
            try {
                Address address = geocoder.getFromLocation(location.latitude, location.longitude, 1).get(0);
                return address.getAddressLine(0) + ", " + address.getLocality() + "\n" + address.getLatitude() + ", " + address.getLongitude();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return location.latitude + ", " + location.longitude;
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        if (marker.equals(addedMarker) || marker.equals(myLocationMarker) || marker.equals(centralMarker)) {
            if (marker.getPosition().equals(centralLocation)) {
                if (distanceEditText == null) {
                    initDistanceEditText();
                }
                new AlertDialog.Builder(this)
                        .setMessage("Zmień zasięg [m]")
                        .setView(distanceEditText)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String newDistance = distanceEditText.getText().toString();
                                if (!TextUtils.isDigitsOnly(newDistance)) {
                                    Toast.makeText(getApplicationContext(), "Proszę wpisać liczbę", Toast.LENGTH_SHORT).show();
                                    return;
                                } else if (!checkInternet())
                                    return;
                                distanceString = distanceEditText.getText().toString();
                                distance = Double.valueOf(distanceString);
                                currentLocationCircle.setRadius(distance);
                                updateApis();
                            }
                        })
                        .setNegativeButton("Wyjdź", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .create()
                        .show();
            } else if (checkInternet()) {
                centralLocation = marker.getPosition();
                if (!centralMarker.equals(myLocationMarker))
                    centralMarker.remove();
                updateApis();
                marker.hideInfoWindow();
            }
        } else {
            float[] distance = new float[1];
            LatLng markerLocation = marker.getPosition();
            Location.distanceBetween(myLastLocation.latitude, myLastLocation.longitude, markerLocation.latitude, markerLocation.longitude, distance);
            String[] splitted = marker.getSnippet().split("\n");
            String duration = null;
            if (splitted.length > 2) {
                duration = splitted[3].split(": ")[1];
            }
            if (messagerMap.containsKey(marker.getId())) {
                messager = messagerMap.get(marker.getId());
                messager.updateAndShow();
            } else {
                messager = new Messager(this, marker.getTitle(), splitted[0], duration);
                messagerMap.put(marker.getId(), messager);
            }
        }
    }

    private void initDistanceEditText() {
        distanceEditText = new EditText(this);
        distanceEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        distanceEditText.setMaxLines(1);
        distanceEditText.setEms(7);
        distanceEditText.setGravity(Gravity.CENTER);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        if (addedMarker != null && !marker.equals(addedMarker) && !addedMarker.getPosition().equals(centralLocation))
            addedMarker.remove();
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (addedMarker != null) {
            if (addedMarker.getPosition().equals(centralLocation)) {
                centralMarker = addedMarker;
                centralMarker.setTitle(getString(R.string.central_location));
            } else {
                addedMarker.remove();
            }
        }
        addedMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(getString(R.string.choose_new))
                .snippet(writeAddressAndLocation(latLng))
                .icon(BitmapDescriptorFactory.defaultMarker(ADDED_MARKER_HUE)));
        addDistanceToAddedMarker();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (apiExes == null) {
            init();
        } else if (checkDistance(myLocationMarker.getPosition(), centralLocation)) {
            centralMarker = myLocationMarker;
            centralMarker.setTitle(getString(R.string.central_location));
            centralMarker.setIcon(BitmapDescriptorFactory.defaultMarker(ADDED_MARKER_HUE));
        } else {
            myLocationMarker.remove();
        }
        markMyLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private boolean checkDistance(LatLng l1, LatLng l2) {
        float[] distance = new float[1];
        Location.distanceBetween(l1.latitude, l2.longitude, l2.latitude, l2.longitude, distance);
        return distance[0] > 0;
    }

    private void init() {
        Intent intent = getIntent();
        distanceString = intent.getStringExtra("distance");
        ArrayList<Integer> checkedPlaces = intent.getIntegerArrayListExtra("checkedPlaces");
        int apiSeparator = intent.getIntExtra("apiSeparator", 1);
        distance = Double.valueOf(distanceString);
        if (animateMyLocation())
            initApis(checkedPlaces, apiSeparator, distanceString, myLastLocation);
    }

    private void addDistanceToAddedMarker() {
        if (!checkInternet())
            return;
        final LatLng location = addedMarker.getPosition();
        String query = "origins=" + centralLocation.latitude + "," + centralLocation.longitude +
                "&destinations=" + location.latitude + "," + location.longitude;
        new AsyncTask<String, Void, JsonNode>() {
            @Override
            protected JsonNode doInBackground(String... params) {
                try {
                    return new ObjectMapper().readTree(new URL(APIExecutor.getGoogleDistanceMatrixURL(params[0])));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JsonNode response) {
                JsonNode result = response.get("rows").get(0).get("elements").get(0);
                StringBuilder sb = new StringBuilder(addedMarker.getSnippet());
                sb.append("\n" + "Dystans pieszo: ");
                sb.append(result.get("distance").get("value").asText() + " m");
                sb.append("\n" + "Czas podróży: ");
                sb.append(result.get("duration").get("text").asText());
                addedMarker.setSnippet(sb.toString());
            }
        }.execute(query);
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        if (marker.equals(addedMarker)) {
            try {
                startActivityForResult(new PlacePicker.IntentBuilder()
                        .setLatLngBounds(toBounds(marker.getPosition(), distance))
                        .build(this), PLACE_PICKER_REQUEST_CODE);
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        } else if (addedMarker != null && !addedMarker.getPosition().equals(centralLocation))
            addedMarker.remove();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                LatLng pickedPosition = PlacePicker.getPlace(this, data).getLatLng();
                addedMarker.setPosition(pickedPosition);
                addedMarker.setSnippet(writeAddressAndLocation(pickedPosition));
                addDistanceToAddedMarker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Messager.SEND_SMS_REQUEST_CODE) {
            if (permissions.length == 1 && permissions[0].equals(Manifest.permission.SEND_SMS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                messager.sendDirectSMS();
            } else {
                messager.updateAndShow();
            }
        }
    }

    private boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnected();
    }

    private boolean checkInternet() {
        if (!isOnline()) {
            Snackbar.make(findViewById(R.id.maps_activity), "Brak połączenia z Internetem", Snackbar.LENGTH_LONG)
                    .setAction("Połącz", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                            manager.setWifiEnabled(true);
                            if (!isOnline()) {
                                startActivity(new Intent((Settings.ACTION_SETTINGS)));
                            }
                        }
                    }).show();
            return false;
        }
        return true;
    }

}