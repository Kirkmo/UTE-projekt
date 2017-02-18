package com.example.zbyszek.ute;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class APIExecutor extends AsyncTask<String, Void, String>  {

    private static Context context;
    private static String UMWarszawaApiKey;
    private static String OrangeApiKey;
    private static String GoogleApiKey;

    private JsonNode response;
    private String url;
    private int PlaceId;
    private String ApiId;
    private float markerId;
    private List<NearbyPlace> nearbyPlaces;
    private GoogleMap mMap;
    private List<Marker> markers;

    public static void setContextAndApis(Context con) {
        context = con;
        UMWarszawaApiKey = context.getString(R.string.um_warszawa_api_key);
        OrangeApiKey = context.getString(R.string.orange_api_key);
        GoogleApiKey = context.getString(R.string.google_maps_key);
    }

    public APIExecutor(String message, String receiver, String sender) {
        makeOrangeURL(message, receiver, sender);
    }

    public APIExecutor(GoogleMap map, LatLng location, String distance, int placeId, float markerId) {
        mMap = map;
        PlaceId = placeId;
        this.markerId = markerId;
        setURL(location,distance);
    }

    private void setURL(LatLng location, String distance) {
        if (canUseApiUMWwa(PlaceId)) {
            makeUMWarszawaURL(location,distance);
        } else {
            makeGooglePlacesURL(location,distance);
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            response = getHttp();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return checkApiStatus();
    }

    @Override
    protected void onPostExecute(String ApiStatus) {
        if (ApiStatus.equals("OK") || ApiStatus.isEmpty()) {
            loadNearbyPlaces(getNearbyPlacesSize());
        } else {
            Toast.makeText(context, ApiStatus, Toast.LENGTH_SHORT).show();
        }
    }

    private JsonNode getHttp() throws IOException {
        return new ObjectMapper().readTree(new URL(url));
    }

    private boolean canUseApiUMWwa(int placeId) {
        return placeId >= 100 ? true : false;
    }

    private String checkApiStatus() {
        if (ApiId.equals(OrangeApiKey))
            return response.get("deliveryStatus").asText();
        return ApiId.equals(UMWarszawaApiKey)
                ? response.get("result").asText()
                : response.get("status").asText();
    }

    private int getNearbyPlacesSize() {
        return ApiId.equals(UMWarszawaApiKey)
                ? response.at("/result/featureMemberProperties").size()
                : response.at("/results").size();
    }

    private void loadNearbyPlaces(int size) {
        nearbyPlaces = new ArrayList<>();
        markers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NearbyPlace np = getNearbyPlace(i);
            markLocation(np);
            nearbyPlaces.add(np);
        }
    }

    private NearbyPlace getNearbyPlaceUMWwa(int i) {
        JsonNode data = response.at("/result/featureMemberProperties").get(i);
        String name = getDataByParam(data,"OPIS");
        String street = getDataByParam(data,"ULICA");
        String number = getDataByParam(data,"NUMER");
        String city = getDataByParam(data,"JEDN_ADM");
        String address = street + " " + number + ", " + city;
        JsonNode location = response.at("/result/featureMemberCoordinates").get(i);
        double latitude = location.get("latitude").asDouble();
        double longitude = location.get("longitude").asDouble();
        return new NearbyPlace(name, address, latitude, longitude);
    }

    private String getDataByParam(JsonNode data, String param) {
        JsonNode paramData = data.get(param);
        return paramData != null ? paramData.asText() : "";
    }

    private NearbyPlace getNearbyPlaceGoogle(int i) {
        JsonNode result = response.get("results").get(i);
        String name = result.get("name").asText();
        String address = result.get("vicinity").asText();
        JsonNode location = result.get("geometry").get("location");
        double latitude = location.get("lat").asDouble();
        double longitude = location.get("lng").asDouble();
        return new NearbyPlace(name, address, latitude, longitude);
    }

    private NearbyPlace getNearbyPlace(int i) {
        return ApiId.equals(UMWarszawaApiKey) ? getNearbyPlaceUMWwa(i) : getNearbyPlaceGoogle(i);
    }

    private void markLocation(NearbyPlace np) {
        markers.add(
                mMap.addMarker(new MarkerOptions()
                    .title(np.getName())
                    .snippet(np.getAddress())
                    .position(np.getLocation())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerId)))
        );
    }

    public void removeMarkers() {
        if (markers != null) {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
        }
    }

    public void hideMarkers() {
        for (Marker marker : markers) {
            marker.setVisible(false);
        }
    }

    public void showMarkers() {
        for (Marker marker : markers) {
            marker.setVisible(true);
        }
    }

    private void makeUMWarszawaURL(LatLng location, String distance) {
        int placeId = PlaceId - 100;
        String id = context.getResources().getStringArray(R.array.places_keys)[placeId];
        ApiId = UMWarszawaApiKey;
        url = "https://api.um.warszawa.pl/api/action/wfsstore_get/" +
                "?id=" + id +
                "&circle=" + location.longitude + "," + location.latitude + "," + distance +
                "&apikey=" + UMWarszawaApiKey;
    }

    private void makeGooglePlacesURL(LatLng location, String distance) {
        String type = context.getResources().getStringArray(R.array.google_place_types)[PlaceId];
        ApiId = GoogleApiKey;
        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + location.latitude + "," + location.longitude +
                "&radius=" + distance +
                "&type=" + type +
                "&key=" + GoogleApiKey;
    }

    private void makeOrangeURL(String message, String receiver, String sender) {
        ApiId = OrangeApiKey;
        if (sender.isEmpty()) {
            url = "https://apitest.orange.pl/Messaging/v1/SMSOnnet?" +
                    "to=" + receiver +
                    "&msg=" + message +
                    "&apikey=" + OrangeApiKey;
        } else {
            url = "https://apitest.orange.pl/Messaging/v1/SMSOnnet?" +
                    "from=" + sender +
                    "&to=" + receiver +
                    "&msg=" + message +
                    "&apikey=" + OrangeApiKey;
        }
    }

}
