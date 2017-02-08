package com.example.zbyszek.ute;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class APIExecutor extends AsyncTask<String, Void, Object>  {
    private static final String UMWarszawaApiKey = Resources.getSystem().getString(R.string.um_warszawa_api_key);
    private static final String OrangeApiKey = Resources.getSystem().getString(R.string.orange_api_key);

    private JsonNode result;

    private String url;
    private String auth;

    private List<NearbyPlace> nearbyPlaces;
    private GoogleMap mMap;

    public APIExecutor(GoogleMap map, String location, String distance, boolean[] places) {
        if (places[3]) {
            mMap = map;
            Log.d("LOCATION", location);
            url = "https://api.um.warszawa.pl/api/action/wfsstore_get/" +
                    "?id=e26218cb-61ec-4ccb-81cc-fd19a6fee0f8" +
                    "&circle=" + location + "," + distance +
                    "&apikey=" + UMWarszawaApiKey;
        }
    }

    @Override
    protected Object doInBackground(String... params) {
        try {
            getHttp(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public APIExecutor(String url) throws IOException {
        this.url = url;
        getHttp(false);
    }

    public APIExecutor(String url, String login, String password) throws IOException {
        this.url = url;
        auth = login+":"+password;

        getHttp(true);
    }

    private void getHttp(boolean hasAuth) throws IOException {

        URLConnection urlConnection = new URL(url).openConnection();
        if (hasAuth) {
            String authEncoded = Base64.encodeToString(auth.getBytes(),Base64.DEFAULT);
            urlConnection.setRequestProperty("Authorization", "Basic " + authEncoded);
        }
        InputStream in = urlConnection.getInputStream();

        ObjectMapper mapper = new ObjectMapper();
        result = mapper.readTree(in).get("result");
        Log.d("JACKSON",result.toString());

    }

    @Override
    protected void onPostExecute(Object o) {
        showNearbyPlaces();
    }

    private void updateNearbyPlaces() {
        //Theatres only by now
        nearbyPlaces = new ArrayList<>();
        if (result == null || result.asText().length() > 0)
            return;
        int placesCount = result.get("featureMemberProperties").size();
        for (int i = 0; i < placesCount; i++) {
            String name = result.get("featureMemberProperties").get(i).get("OPIS").asText();
            JsonNode location = result.get("featureMemberCoordinates").get(i);
            double latitude = location.get("latitude").asDouble();
            double longitude = location.get("longitude").asDouble();
            nearbyPlaces.add(new NearbyPlace(name, latitude, longitude));
        }
    }

    private void showNearbyPlaces() {
        updateNearbyPlaces();
        if (nearbyPlaces == null)
            return;
        for (NearbyPlace np : nearbyPlaces) {
            mMap.addMarker(new MarkerOptions()
                    .position(np.getLocation())
                    .title(np.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            );
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12.0f));
        }
    }
}
