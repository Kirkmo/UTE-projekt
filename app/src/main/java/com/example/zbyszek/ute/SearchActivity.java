package com.example.zbyszek.ute;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Arrays;


// Activity collecting user preferences about places and distance
public class SearchActivity extends AppCompatActivity {

    final String TAG = "SearchActivity";
    final int CINEMA_INDEX = 0;
    final int STOP_INDEX = 1;
    final int PUB_INDEX = 2;
    final int THEATRE_INDEX = 3;
    final int SHOP_INDEX = 3;
    String distance;
    boolean[] places;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        final EditText editText = (EditText)findViewById(R.id.editTextDistance);



        places = new boolean[5];

        Button button = (Button)findViewById(R.id.searchBtn2);
        button.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view){
                // action
                try{
                    distance = editText.getText().toString();
                }
                catch (NumberFormatException nfe){
                    distance = "1000";
                }



                Bundle bundle = new Bundle();
                bundle.putBooleanArray("places",places);
                bundle.putString("distance", distance);
                Log.d(TAG, " Distance: " + distance + " Places: " + Arrays.toString(places));
                Intent intent = new Intent(SearchActivity.this, MapsActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);


                // how to grab in new activity:
                // value = b.getFloat("distance");
            }
        });


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkBoxCinema:
                if (checked)
                    places[CINEMA_INDEX] = true;
                else
                    places[CINEMA_INDEX] = false;

                break;
            case R.id.checkBoxStop:
                if (checked)
                    places[STOP_INDEX] = true;
                else
                    places[STOP_INDEX] = false;
                break;
            case R.id.checkBoxPub:
                if (checked)
                    places[PUB_INDEX] = true;
                else
                    places[PUB_INDEX] = false;
                break;

            case R.id.checkBoxTheatre:
                if (checked)
                    places[THEATRE_INDEX] = true;
                else
                    places[THEATRE_INDEX] = false;
                break;
            case R.id.checkBoxShop:
                if (checked)
                    places[SHOP_INDEX] = true;
                else
                    places[SHOP_INDEX] = false;
                break;
        }
    }


}
