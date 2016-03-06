/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package be.ndusart.carfinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Dialog;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

public class MainActivity extends Activity implements OnClickListener, OnMapReadyCallback {
	
	private BluetoothAdapter btAdapter;
	private BluetoothStateReceiver btReceiver;
	private Button problemButton;
	private String carBtAddress;
	private TextView carTextView;
	private TextView locationTextView;
	private GoogleMap map;
	
	public static final String CAR_PREFERENCES = "be.ndusart.carfinder.MainActivity.CAR_PREFERENCES";
	public static final String CAR_BT_ADDRESS_KEY = "carBtAddressKey";
	public static final String CAR_LAST_LATITUDE_KEY = "carBtLatitudeKey";
	public static final String CAR_LAST_LONGITUDE_KEY = "carBtLongitudeKey";
	public static final String CAR_LAST_POSITION_STREET_KEY = "carBtStreetKey";
	private static final int REQUEST_ENABLE_BT = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		carBtAddress = null;
		
		problemButton = (Button)findViewById(R.id.problemButton);
		problemButton.setOnClickListener(this);
		
		carTextView = (TextView)findViewById(R.id.carLabel);
		locationTextView = (TextView)findViewById(R.id.locationLabel);
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if( btAdapter == null ) {
			Toast.makeText(this, "Your device does not support bluetooth !", Toast.LENGTH_LONG).show();
		} else {
			SharedPreferences preferences = getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE);
			carBtAddress = preferences.getString(CAR_BT_ADDRESS_KEY, null);
		}
	}
	
	public static String getCarBluetoothAddress(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE);
		return preferences.getString(CAR_BT_ADDRESS_KEY, null);
	}
	
	public static float getLastLatitude(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE);
		return preferences.getFloat(CAR_LAST_LATITUDE_KEY, 0.0f);
	}
	
	public static float getLastLongitude(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE);
		return preferences.getFloat(CAR_LAST_LONGITUDE_KEY, 0.0f);
	}
	
	public static boolean hasLocation(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE);
		
		return (preferences.contains(CAR_LAST_LATITUDE_KEY) && preferences.contains(CAR_LAST_LONGITUDE_KEY));
	}
	
	public static String getLastStreet(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE);
		return preferences.getString(CAR_LAST_POSITION_STREET_KEY, null);
	}
	
	public static void updateLastStreet(String street, Context context) {
		SharedPreferences.Editor edit = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE).edit();
		edit.putString(CAR_LAST_POSITION_STREET_KEY, street);
		edit.commit();
	}
	
	public static void removeLastStreet(Context context) {
		SharedPreferences.Editor edit = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE).edit();
		edit.remove(CAR_LAST_POSITION_STREET_KEY);
		edit.commit();
	}
	
	public static void updatePosition(float latitude, float longitude, Context context) {
		SharedPreferences.Editor edit = context.getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE).edit();
		edit.putFloat(CAR_LAST_LATITUDE_KEY, latitude);
		edit.putFloat(CAR_LAST_LONGITUDE_KEY, longitude);
		edit.commit();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		if( btAdapter!= null ) {
			if( !btAdapter.isEnabled() ) {
				problemButton.setText("Turn on Bluetooth");
				problemButton.setVisibility(View.VISIBLE);
			} else if( !isCarConfigured() ) {
				problemButton.setText("Configure your car");
				problemButton.setVisibility(View.VISIBLE);
			} else {
				showCarName();
			}
		}
		
		btReceiver = new BluetoothStateReceiver();
		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(btReceiver, filter);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;

		map.setPadding(10, 10, 10, 10);
		map.moveCamera(CameraUpdateFactory.zoomTo(16.0f));
		map.setMyLocationEnabled(true);

		if( hasLocation(this) ) {
			float latitude = getLastLatitude(this);
			float longitude = getLastLongitude(this);
			String street = getLastStreet(this);
			locationTextView.setText("Last location: ("+latitude+","+longitude+")");
			locationTextView.setVisibility(View.VISIBLE);
			if( street==null || street.length()==0 ) {
				if(Geocoder.isPresent()) {
					Location last = new Location("be.ndusart");
					last.setLatitude(latitude);
					last.setLongitude(longitude);
					(new GetAddressTask()).execute(last);
				}
			} else {
				locationTextView.setText("Last location: "+street);
			}

			map.clear();
			LatLng carPosition = new LatLng(latitude, longitude);
			map.addMarker(new MarkerOptions().position(carPosition));
			map.moveCamera(CameraUpdateFactory.newLatLng(carPosition));
		} else {
			locationTextView.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Check if Google Play Services is installed
		GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
		int status = gaa.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS) {
			Dialog dialog = gaa.getErrorDialog(this, status, 1);
			dialog.show();
		} else {
			// show map if Google Play Services is present and up-to-date
			MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
			mapFragment.getMapAsync(this);
		}
	}

	@Override
	protected void onStop() {
		unregisterReceiver(btReceiver);
		btReceiver = null;
		
		problemButton.setVisibility(View.GONE);
		super.onStop();
	}

	@Override
	public void onClick(View v) {
		if( v == problemButton && btAdapter != null ) {
			if( !btAdapter.isEnabled() ) {
				enableBluetooth();
			} else {
				configureCar();
			}
		}
	}
	
	private void enableBluetooth() {
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}
	
	private boolean isCarConfigured() {
		boolean configured = false;
		
		if( btAdapter!=null && btAdapter.isEnabled() && carBtAddress != null && carBtAddress.length()>0 ) {
			for (BluetoothDevice device : btAdapter.getBondedDevices()) {
				if( device.getAddress().equals(carBtAddress) ) {
					configured = true;
					break;
				}
			}
		}
		
		return configured;
	}
	
	private void showCarName() {
		if( btAdapter!=null && btAdapter.isEnabled() && carBtAddress != null && carBtAddress.length()>0 ) {
			for (BluetoothDevice device : btAdapter.getBondedDevices()) {
				if( device.getAddress().equals(carBtAddress) ) {
					carTextView.setText("Your car is set to \""+device.getName()+"\"");
					break;
				}
			}
		}
	}
	
	private void configureCar() {
		if( btAdapter==null || !btAdapter.isEnabled() )
			return;
		
		ArrayList<String> btNames = new ArrayList<String>();
		ArrayList<String> btAddresses = new ArrayList<String>();
		
		for (BluetoothDevice device : btAdapter.getBondedDevices()) {
			btNames.add(device.getName());
			btAddresses.add(device.getAddress());
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose your car");
		builder.setItems(btNames.toArray((new String[btNames.size()])), new DialogClickListener(btAddresses));
		builder.create().show();
	}
	
	private class BluetoothStateReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
			
			if( state == BluetoothAdapter.STATE_ON ) {
				if( !isCarConfigured() ) {
					problemButton.setText("Configure your car");
					problemButton.setVisibility(View.VISIBLE);
				} else {
					problemButton.setVisibility(View.GONE);
				}
			} else {
				problemButton.setText("Turn on Bluetooth");
				problemButton.setVisibility(View.VISIBLE);
			}
		}
		
	}
	
	private class DialogClickListener implements DialogInterface.OnClickListener {
		
		private ArrayList<String> btAddresses;
		
		public DialogClickListener(ArrayList<String> addresses) {
			btAddresses = new ArrayList<String>(addresses);
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			carBtAddress = btAddresses.get(which);
			SharedPreferences preferences = getSharedPreferences(CAR_PREFERENCES, MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CAR_BT_ADDRESS_KEY, carBtAddress);
			editor.commit();
			
			problemButton.setVisibility(View.GONE);
			
			showCarName();
		}
		
	}
	
	
	private class GetAddressTask extends AsyncTask<Location, Void, String> {

		@Override
		protected String doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(MainActivity.this);
			Location loc = params[0];
			List<Address> addresses = null;
			
			try {
				addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
			} catch (IOException e) {
				return null;
			}
			
			if( addresses != null && addresses.size() > 0 ) {
				Address addr = addresses.get(0);
				String addressText = String.format(
									"%s, %s, %s",
									// If there's a street address, add it
									addr.getMaxAddressLineIndex() > 0 ?
											addr.getAddressLine(0) : "",
									// Locality is usually a city
									addr.getLocality(),
									// The country of the address
									addr.getCountryName());
				updateLastStreet(addressText, MainActivity.this);
				return addressText;
			}
			
			return null;
		}
		
		
		@Override
		protected void onPostExecute(String result) {
			if( result != null && result.length() > 0 ) {
				locationTextView.setText("Last location: "+result);
			}
		}
	}
}
