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
	private static int MAX_WAIT_LOCATION_UPDATE = 60000; // accept an update until 60 sec after connection loss
	private long disconnectTime;
	private LocationManager locationManager;
	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String car = MainActivity.getCarBluetoothAddress(context);
		
		if( car==null || car.length()==0 || intent.getAction()!=BluetoothDevice.ACTION_ACL_DISCONNECTED )
			return;
		
		BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		
		if( device != null && device.getAddress().equals(car) ) {
			Toast.makeText(context, "Going out of car, storing location...", Toast.LENGTH_LONG).show();
			MainActivity.removeLastStreet(context);
			updatePosition(context);
		}
	}
	
	private void updatePosition(Context context) {
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		mContext = context;
		
		Location lastNetworkPosition = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location lastGPSPosition = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		long now = new Date().getTime();
		
		double latitude=0.0, longitude=0.0;
		
		if ( lastGPSPosition != null ) {
			latitude = lastGPSPosition.getLatitude();
			longitude = lastGPSPosition.getLongitude();
		} else if ( lastNetworkPosition != null ) {
			latitude = lastNetworkPosition.getLatitude();
			longitude = lastNetworkPosition.getLongitude();
		}
		
		if( latitude != 0.0 || longitude != 0.0 ) {
			MainActivity.updatePosition((float)latitude, (float)longitude, context);
		}
		
		disconnectTime = now;
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
		
		if( provider == LocationManager.GPS_PROVIDER ) {
			// always use the new position if from GPS
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			MainActivity.updatePosition((float)latitude, (float)longitude, mContext);
			stopLocationUpdates(); // GPS is enough accurate, stop updates
		} else if( provider == LocationManager.NETWORK_PROVIDER ) {
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			MainActivity.updatePosition((float)latitude, (float)longitude, mContext);
			
			if( ! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
				stopLocationUpdates(); // stop now as GPS is off, we won't get any better location
			}
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
