package com.example.zbyszek.ute;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlacesChooserActivity extends AppCompatActivity {

    private static final int MY_LOCATION_REQUEST_CODE = 0;
    private List<String> places;
    private List<String> googlePlaces;
    private ArrayList<Integer> googlePlacesIndexesAdded;
    private boolean[] placesAdded;
    private int placesCounter = 0;
    private Set<Integer> placesToAdd = new HashSet<>();
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private EditText distanceText;
    private AlertDialog placesAdderWindow;
    private Intent mapIntent;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.place_chooser_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_chooser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        googlePlaces = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.google_places)));
        placesAdded = new boolean[googlePlaces.size()];
        places = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.places)));

        distanceText = (EditText) findViewById(R.id.distEditText);
        mListView = (ListView) findViewById(R.id.placesListView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListView.isItemChecked(position)) {
                    if (placesCounter < 10) {
                        placesCounter++;
                    } else {
                        mListView.setItemChecked(position, false);
                    }
                } else {
                    placesCounter--;
                }
                Toast.makeText(getApplicationContext(), "Wybrano " + placesCounter + " z 10 typów", Toast.LENGTH_SHORT).show();
            }
        });
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, places) {
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (position < getResources().getStringArray(R.array.places_keys).length){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Drawable background = getDrawable(R.drawable.dane_po_warszawsku);
                        background.setAlpha(100);
                        view.setBackground(background);
                    } else
                        view.setBackgroundColor(Color.LTGRAY);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.setBackground(mListView.getBackground());
                }
                return view;
            }
        };
        mListView.setAdapter(mAdapter);
        initGooglePlacesDefault();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchOnMap();
            }
        });
    }

    private void initGooglePlacesDefault() {
        googlePlacesIndexesAdded = new ArrayList<>();
        for (int i = getResources().getStringArray(R.array.places_keys).length; i < places.size(); i++) {
            String placeName = mAdapter.getItem(i);
            int index = googlePlaces.indexOf(placeName);
            googlePlacesIndexesAdded.add(index);
            placesAdded[index] = true;
        }
    }

    public void menuItemClick(MenuItem mu) {
        if (placesAdderWindow == null) {
            buildPlacesAdderWindow();
        }
        placesAdderWindow.show();
    }

    private void buildPlacesAdderWindow() {
        placesAdderWindow = new AlertDialog.Builder(this)
                .setTitle(R.string.add_places)
                .setMultiChoiceItems(R.array.google_places, placesAdded, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (googlePlacesIndexesAdded.contains(which)) {
                            placesAdded[which] = true;
                        } else if (isChecked) {
                            placesToAdd.add(which);
                        } else if (placesToAdd.contains(which)) {
                            placesToAdd.remove(which);
                        }
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addPlaces();
                    }
                })
                .setNegativeButton("Wyjdź", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        placesAdderWindow.show();
    }

    private void addPlaces() {
        for (int i : placesToAdd) {
            mAdapter.add(getResources().getStringArray(R.array.google_places)[i]);
            if (placesCounter < 10) {
                mListView.setItemChecked(mListView.getCount() - 1, true);
                placesCounter++;
            }
        }
        Toast.makeText(getApplicationContext(), "Wybrano " + placesCounter + " z 10 typów", Toast.LENGTH_SHORT).show();
        googlePlacesIndexesAdded.addAll(placesToAdd);
        placesToAdd.clear();
    }

    public void searchOnMap() {
        String distance = distanceText.getText().toString();
        if (distance.isEmpty()) {
            distance = distanceText.getHint().toString();
        } else if (!TextUtils.isDigitsOnly(distance)) {
            Toast.makeText(this,"Proszę wpisać liczbę",Toast.LENGTH_LONG);
            return;
        }
        Bundle bundle = new Bundle();
        ArrayList<Integer> checkedPlaces = new ArrayList<>();
        int apiSeparator = 0;
        int UMWWaSize = getResources().getStringArray(R.array.places_keys).length;
        for (int i = 0; i < mListView.getCount(); i++) {
            if (mListView.isItemChecked(i)) {
                if (i < UMWWaSize) {
                    checkedPlaces.add(i + 100);
                    apiSeparator++;
                } else {
                    checkedPlaces.add(googlePlacesIndexesAdded.get(i - UMWWaSize));
                }
            }
        }
        bundle.putIntegerArrayList("checkedPlaces", checkedPlaces);
        bundle.putInt("apiSeparator", apiSeparator);
        bundle.putString("distance",distance);
        mapIntent = new Intent(PlacesChooserActivity.this, MapsActivity.class);
        mapIntent.putExtras(bundle);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] permissions = new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions, MY_LOCATION_REQUEST_CODE);
            }
        } else {
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Snackbar.make(findViewById(R.id.content_places_chooser),"Proszę włączyć GPS", Snackbar.LENGTH_LONG)
                        .setAction("Włącz GPS", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        }).show();
            } else
                startActivity(mapIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 && permissions[0].equals(android.Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Snackbar.make(findViewById(R.id.content_places_chooser), "Proszę włączyć GPS", Snackbar.LENGTH_LONG)
                    .setAction("Włącz GPS", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    }).show();
                } else {
                    startActivity(mapIntent);
                }
            }
            else {
                Toast.makeText(this, "Tryb bez GPSu", Toast.LENGTH_LONG).show();
                startActivity(mapIntent);
            }
        }
    }
}
