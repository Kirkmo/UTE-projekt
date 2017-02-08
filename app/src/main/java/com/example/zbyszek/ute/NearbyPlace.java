package com.example.zbyszek.ute;

import com.google.android.gms.maps.model.LatLng;

public class NearbyPlace {
	private final String name;
	private final LatLng location;
	
	public NearbyPlace(String name, double latitude, double longitude) {
		this.name = name;
		location = new LatLng(latitude, longitude);
	}

	public String getName() {
		return name;
	}

	public LatLng getLocation() {
		return location;
	}

	public String toString() {
		return name + " (" + location.latitude + ", " + location.longitude + ")";
	}

}
