package com.example.zbyszek.ute;

import com.google.android.gms.maps.model.LatLng;

public class NearbyPlace {
	private final String name;
	private final String address;
	private final LatLng location;
	private String distance;
	private String duration;

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

	public String getLocationAsText() {
		return location.latitude + "," + location.longitude;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(address + "\n" +
				location.latitude + ", " + location.longitude);
		if (distance != null && duration != null) {
			sb.append("\n" + "Dystans pieszo: ");
			sb.append(distance);
			sb.append("\n" + "Czas podróży: ");
			sb.append(duration);
		}
		return sb.toString();
	}

	public String getDistance() {
		return distance;
	}

	public void setDistance(String distance) {
		this.distance = distance + " m";
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
}
