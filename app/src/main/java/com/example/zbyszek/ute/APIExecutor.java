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

public class APIExecutor {

    private static Context context;
    private static String UMWarszawaApiKey;
    private static String OrangeApiKey;
    private static String GoogleApiKey;

    private static final String UMWarszawa = "UM Warszawa";
    private static final String GooglePlaces = "Google Places";
    private static final String GoogleDistanceMatrix = "Google Distance Matrix";
    private static final String OrangeAPI = "Orange API";

    private String centralLocation;
    private String url;
    private int PlaceId;
    private String placeName;
    private String ApiName;
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
        centralLocation = location.latitude + "," + location.longitude;
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

    private AsyncTask<String,Boolean,String> getAsyncTask() {
        return new AsyncTask<String,Boolean,String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    JsonNode response = getHttp();
                    String ApiStatus = getApiStatus(response);
                    if (ApiStatus.equals("OK") || ApiStatus.isEmpty()) {
                        String nearbyLocationsQuery = loadNearbyPlaces(response, getNearbyPlacesSize(response));
                        setNearbyPlacesDistance(getHttp(getGoogleDistanceMatrixURL(nearbyLocationsQuery)));
                    } else {
                        return ApiStatus;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String ApiStatus) {
                if (ApiStatus != null) {
                    Toast.makeText(context, placeName + " [" + ApiName + "]: " + ApiStatus, Toast.LENGTH_SHORT).show();
                } else {
                    removeMarkers();
                    for (NearbyPlace np : nearbyPlaces) {
                        markLocation(np);
                    }
                }
            }
        };
    }

    public void execute() {
        getAsyncTask().execute();
    }

    private JsonNode getHttp(String url) throws IOException {
        return new ObjectMapper().readTree(new URL(url));
    }

    private JsonNode getHttp() throws IOException {
        return new ObjectMapper().readTree(new URL(url));
    }

    private boolean canUseApiUMWwa(int placeId) {
        return placeId >= 100 ? true : false;
    }

    private String getApiStatus(JsonNode response, boolean distanceReq) {
        if (distanceReq)
            return GoogleDistanceMatrix;
        if (ApiName.equals(OrangeAPI))
            return response.get("deliveryStatus").asText();
        return ApiName.equals(UMWarszawa)
                ? response.get("result").asText()
                : response.get("status").asText();
    }

    private String getApiStatus(JsonNode response) {
        return getApiStatus(response, false);
    }

    private int getNearbyPlacesSize(JsonNode response) {
        return ApiName.equals(UMWarszawa)
                ? response.at("/result/featureMemberProperties").size()
                : response.at("/results").size();
    }

    private String loadNearbyPlaces(JsonNode response, int size) {
        nearbyPlaces = new ArrayList<>();
        StringBuilder nearbyLocationsQuery = new StringBuilder("origins=" + centralLocation);
        nearbyLocationsQuery.append("&destinations=");
        for (int i = 0; i < size; i++) {
            NearbyPlace np = getNearbyPlace(response,i);
            nearbyLocationsQuery.append(np.getLocationAsText());
            if (i != size - 1) nearbyLocationsQuery.append("|");
            nearbyPlaces.add(np);
        }
        return nearbyLocationsQuery.toString();
    }

    private void setNearbyPlacesDistance(JsonNode response) {
        for (int i = 0; i < nearbyPlaces.size(); i++) {
            JsonNode result = response.get("rows").get(0).get("elements").get(i);
            nearbyPlaces.get(i).setDistance(result.get("distance").get("value").asText());
            nearbyPlaces.get(i).setDuration(result.get("duration").get("text").asText());
        }
    }

    private NearbyPlace getNearbyPlaceUMWwa(JsonNode response, int i) {
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

    private NearbyPlace getNearbyPlaceGoogle(JsonNode response, int i) {
        JsonNode result = response.get("results").get(i);
        String name = result.get("name").asText();
        String address = result.get("vicinity").asText();
        JsonNode location = result.get("geometry").get("location");
        double latitude = location.get("lat").asDouble();
        double longitude = location.get("lng").asDouble();
        return new NearbyPlace(name, address, latitude, longitude);
    }

    private NearbyPlace getNearbyPlace(JsonNode response, int i) {
        return ApiName.equals(UMWarszawa) ? getNearbyPlaceUMWwa(response,i) : getNearbyPlaceGoogle(response,i);
    }

    private void markLocation(NearbyPlace np) {
        String title = np.getName();
        if (title.isEmpty())
            title = placeName;
        markers.add(
                mMap.addMarker(new MarkerOptions()
                    .title(title)
                    .snippet(np.toString())
                    .position(np.getLocation())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerId)))
        );
    }

    public void removeMarkers() {
        if (markers != null) {
            for (Marker marker : markers) {
                marker.remove();
            }
        }
        markers = new ArrayList<>();
    }

    public void hideMarkers() {
        if (markers != null) {
            for (Marker marker : markers) {
                marker.setVisible(false);
            }
        }
    }

    public void showMarkers() {
        if (markers != null) {
            for (Marker marker : markers) {
                marker.setVisible(true);
            }
        }
    }

    private void makeUMWarszawaURL(LatLng location, String distance) {
        int placeId = PlaceId - 100;
        String id = context.getResources().getStringArray(R.array.places_keys)[placeId];
        placeName = context.getResources().getStringArray(R.array.places)[placeId];
        ApiName = UMWarszawa;
        url = "https://api.um.warszawa.pl/api/action/wfsstore_get/" +
                "?id=" + id +
                "&circle=" + location.longitude + "," + location.latitude + "," + distance +
                "&apikey=" + UMWarszawaApiKey;
    }

    private void makeGooglePlacesURL(LatLng location, String distance) {
        String type = context.getResources().getStringArray(R.array.google_place_types)[PlaceId];
        placeName = context.getResources().getStringArray(R.array.google_places)[PlaceId];
        ApiName = GooglePlaces;
        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + location.latitude + "," + location.longitude +
                "&radius=" + distance +
                "&type=" + type +
                "&key=" + GoogleApiKey;
    }

    private void makeOrangeURL(String message, String receiver, String sender) {
        ApiName = OrangeAPI;
        StringBuilder urlSB = new StringBuilder("https://apitest.orange.pl/Messaging/v1/SMSOnnet?");
        if (!sender.isEmpty()) urlSB.append("from=" + sender + "&");
        urlSB.append("to=" + receiver);
        urlSB.append("&msg=" + message);
        urlSB.append("&apikey=" + OrangeApiKey);
        url = urlSB.toString();
    }

    public static String getGoogleDistanceMatrixURL(String placesLocationsQuery) {
        return "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                placesLocationsQuery +
                "&mode=walking" +
                "&language=pl-PL" +
                "&key=" + GoogleApiKey;
    }

    public void update(LatLng location, String distance) {
        centralLocation = location.latitude + "," + location.longitude;
        setURL(location,distance);
        execute();
    }
}
