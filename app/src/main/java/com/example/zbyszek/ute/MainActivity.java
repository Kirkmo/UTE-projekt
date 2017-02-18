package com.example.zbyszek.ute;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void click(View view) {
        Intent intent;

        switch (view.getId()){

            case R.id.searchBtn:
                intent = new Intent(MainActivity.this, PlacesChooserActivity.class);//SearchActivity.class);
                startActivity(intent);

            break;
        }
    }
}
