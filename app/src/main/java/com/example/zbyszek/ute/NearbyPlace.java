package com.example.zbyszek.ute;

import com.google.android.gms.maps.model.LatLng;

public class NearbyPlace {
	private final String name;
	private final String address;
	private final LatLng location;

	public NearbyPlace(String name, String address, double latitude, double longitude) {
		this.name = name;
		this.address = address;
		location = new LatLng(latitude, longitude);
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public LatLng getLocation() {
		return location;
	}

	public String toString() {
		return name + " -> " + address + " (" + location.latitude + ", " + location.longitude + ")";
	}

}
