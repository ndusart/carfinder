/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package be.ndusart.carfinder;

import java.util.Date;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public class CarDetecter extends BroadcastReceiver implements LocationListener {
	private static final int MAX_WAIT_LOCATION_UPDATE = 60000; // accept an update until 60 sec after connection loss
	private long disconnectTime;
	private long connectTime;
	private float lastAccuracy;
	private LocationManager locationManager;
	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String car = MainActivity.getCarBluetoothAddress(context);
		
		if( car==null || car.length()==0 )
			return;
		
		if( !intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED) && !intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED) )
			return;
		
		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		
		if( device != null && device.getAddress().equals(car) ) {
			
			if( intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED) ) {
				Toast.makeText(context, "Going out of car, storing location...", Toast.LENGTH_LONG).show();
				MainActivity.removeLastStreet(context);
				updatePosition(context);
			}
			else {
				connectTime = new Date().getTime();
			}
		}
	}
	
	private void updatePosition(Context context) {
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		mContext = context;
		
		Location lastNetworkPosition = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location lastGPSPosition = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		long now = new Date().getTime();
		
		double latitude=0.0, longitude=0.0;
		
		if ( lastGPSPosition != null && lastGPSPosition.getTime() > connectTime ) {
			latitude = lastGPSPosition.getLatitude();
			longitude = lastGPSPosition.getLongitude();
		} else if ( lastNetworkPosition != null && lastNetworkPosition.getTime() > connectTime ) {
			latitude = lastNetworkPosition.getLatitude();
			longitude = lastNetworkPosition.getLongitude();
		}
		
		if( latitude != 0.0 || longitude != 0.0 ) {
			MainActivity.updatePosition((float)latitude, (float)longitude, context);
		}
		
		disconnectTime = now;
		lastAccuracy = -1.0f;
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0.0f, this);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, this);
	}

	@Override
	public void onLocationChanged(Location location) {
		if( mContext == null ) // location updates were stopped
			return;
		
		String provider = location.getProvider();
		
		if( (location.getTime() - disconnectTime) > MAX_WAIT_LOCATION_UPDATE )
		{
			// update took too long, discard and stop updates
			stopLocationUpdates();
			return;
		}
		
		float accuracy = location.getAccuracy();
		
		if( accuracy >= 0.0f && accuracy < lastAccuracy )
			return; // discard this update as it is less accurate than the last one
		
		if( provider != null && provider.equals(LocationManager.GPS_PROVIDER) ) {
			// always use the new position if from GPS
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			MainActivity.updatePosition((float)latitude, (float)longitude, mContext);
		} else if( provider != null && provider.equals(LocationManager.NETWORK_PROVIDER) ) {
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			MainActivity.updatePosition((float)latitude, (float)longitude, mContext);
		} else {
			return;
		}
		
		if( accuracy < 50.0f ) {
			stopLocationUpdates(); // accurate enough, stop updates
		}
	}
	
	private void stopLocationUpdates() {
		locationManager.removeUpdates(this);
		mContext = null;
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
