package com.example.zbyszek.ute;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;


// Activity collecting user preferences about places and distance
public class SearchActivity extends AppCompatActivity {

    float distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        String[] places = {"Kino", "Pub", "Przystanek", "Bankomat"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, places);
        Spinner spinner = (Spinner)findViewById(R.id.spinnerPlaces);
        spinner.setAdapter(adapter);

        EditText editText = (EditText)findViewById(R.id.editTextDistance);



        Button button = (Button)findViewById(R.id.searchBtn2);
        button.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view){
                // action
            }
        });


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
