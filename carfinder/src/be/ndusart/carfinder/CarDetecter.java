/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package be.ndusart.carfinder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

public class CarDetecter extends BroadcastReceiver implements ConnectionCallbacks, OnConnectionFailedListener {
	
	private LocationClient mLocation;
	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String car = MainActivity.getCarBluetoothAddress(context);
		
		if( car==null || car.length()==0 || intent.getAction()!=BluetoothDevice.ACTION_ACL_DISCONNECTED )
			return;
		
		BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		
		if( device.getAddress().equals(car) ) {
			Toast.makeText(context, "Going out of car, storing location...", Toast.LENGTH_LONG).show();
			MainActivity.removeLastStreet(context);
			updatePosition(context);
		}
	}
	
	private void updatePosition(Context context) {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		
		if( resultCode != ConnectionResult.SUCCESS )
			return;
		
		mLocation = new LocationClient(context, this, this);
		
		mContext = context;
		
		mLocation.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		mContext = null;
		mLocation = null;
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Location location = mLocation.getLastLocation();
		
		MainActivity.updatePosition((float)location.getLatitude(), (float)location.getLongitude(), mContext);
		
		mContext = null;
		mLocation.disconnect();
		mLocation = null;
	}

	@Override
	public void onDisconnected() {
		mContext = null;
		mLocation = null;
	}

}
