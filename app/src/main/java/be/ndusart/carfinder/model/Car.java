package be.ndusart.carfinder.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import be.ndusart.carfinder.provider.CarContentProvider;

public class Car {

    private long id;
    private String name;
    private String macAddress;
    private int color;
    private LatLng coordinates;

    public Car() {
        id = -1;
        name = null;
        macAddress = null;
        color = Color.BLACK;
        coordinates = null;
    }

    public Car(String name, String macAddress, int color, double latitude, double longitude) {
        this(name, macAddress, color, new LatLng(latitude, longitude));
    }

    public Car(String name, String macAddress, int color, LatLng coordinates) {
        this();
        this.name = name;
        this.macAddress = macAddress;
        this.color = color;
        this.coordinates = coordinates;
    }

    public Car(long id, Context context) throws Exception {
        this.id = id;
        refresh(context);
    }

    public Car(Cursor cursor, int id_index, int name_index, int mac_address_index, int color_index, int latitude_index, int longitude_index) {
        this();

        if( id_index < 0 )
            id_index = cursor.getColumnIndex(DatabaseOpenHelper.CARS_ID_COLUMN);

        if( name_index < 0 )
            name_index = cursor.getColumnIndex(DatabaseOpenHelper.CARS_NAME_COLUMN);

        if( mac_address_index < 0 )
            mac_address_index = cursor.getColumnIndex(DatabaseOpenHelper.CARS_BT_MAC_COLUMN);

        if( color_index < 0 )
            color_index = cursor.getColumnIndex(DatabaseOpenHelper.CARS_COLOR_COLUMN);

        if( latitude_index < 0 )
            latitude_index = cursor.getColumnIndex(DatabaseOpenHelper.CARS_LATITUDE_COLUMN);

        if( longitude_index < 0 )
            longitude_index = cursor.getColumnIndex(DatabaseOpenHelper.CARS_LONGITUDE_COLUMN);

        id = cursor.getLong(id_index);
        name = cursor.getString(name_index);
        macAddress = cursor.getString(mac_address_index);
        color = cursor.getInt(color_index);
        coordinates = new LatLng(cursor.getDouble(latitude_index), cursor.getDouble(longitude_index));
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public void save(Context context) throws Exception {
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(DatabaseOpenHelper.CARS_BT_MAC_COLUMN, macAddress);
        values.put(DatabaseOpenHelper.CARS_NAME_COLUMN, name);
        values.put(DatabaseOpenHelper.CARS_COLOR_COLUMN, color);
        if( coordinates != null ) {
            values.put(DatabaseOpenHelper.CARS_LATITUDE_COLUMN, coordinates.latitude);
            values.put(DatabaseOpenHelper.CARS_LONGITUDE_COLUMN, coordinates.longitude);
        } else {
            values.put(DatabaseOpenHelper.CARS_LATITUDE_COLUMN, (String)null);
            values.put(DatabaseOpenHelper.CARS_LONGITUDE_COLUMN, (String)null);
        }

        if( id < 0 ) {
            // new entity
            try {
                Uri uri = resolver.insert(CarContentProvider.carsUri(), values);

                if(uri == null)
                    throw new Exception("failed to retrieve new uri");

                try {
                    id = Long.parseLong(uri.getLastPathSegment());
                } catch( NumberFormatException e ) {
                    throw new Exception("the returned uri is not a car uri (" + uri  + ")");
                }
            } catch (Exception e) {
                throw new Exception("Error creating car: " + e.getMessage());
            }
        } else {
            // update existing entity
            int count = resolver.update(CarContentProvider.carUri(id), values, null, null);

            if( count == 0 ) {
                throw new Exception("Error updating car with id " + id + ": car not found");
            }
        }
    }

    public void refresh(Context context) throws Exception {
        if( id < 0 )
            return; // not in DB, nothing to refresh

        ContentResolver resolver = context.getContentResolver();

        String[] projection = new String[]{
                DatabaseOpenHelper.CARS_NAME_COLUMN,
                DatabaseOpenHelper.CARS_BT_MAC_COLUMN,
                DatabaseOpenHelper.CARS_COLOR_COLUMN,
                DatabaseOpenHelper.CARS_LATITUDE_COLUMN,
                DatabaseOpenHelper.CARS_LONGITUDE_COLUMN
        };

        int name_index = 0;
        int address_index = 1;
        int color_index = 2;
        int latitude_index = 3;
        int longitude_index = 4;

        Cursor cursor = resolver.query(CarContentProvider.carUri(id), projection, null, null, null);

        if( cursor == null )
            throw new Exception("Failed to retrieve car with id " + id + ": null cursor");

        try {
            if (!cursor.moveToFirst()) {
                throw new Exception("Failed to refresh car with id " + id + ": car not found");
            }

            name = cursor.getString(name_index);
            macAddress = cursor.getString(address_index);
            color = cursor.getInt(color_index);
            coordinates = new LatLng(cursor.getDouble(latitude_index), cursor.getDouble(longitude_index));
        } finally {
            cursor.close();
        }
    }

    public void delete(Context context) {
        if( id < 0 )
            return;

        ContentResolver resolver = context.getContentResolver();
        resolver.delete(CarContentProvider.carUri(id), null, null);

        id = -1;
    }

    public static int numberOfCars(Context context) {
        ContentResolver resolver = context.getContentResolver();

        // get all cars
        Cursor cursor = resolver.query(CarContentProvider.carsUri(), null, null, null, null);

        if( cursor == null )
            return 0;

        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    public static ArrayList<Car> getAllCars(Context context) {
        ArrayList<Car> cars = new ArrayList<>();

        ContentResolver resolver = context.getContentResolver();

        String[] projection = new String[]{
                DatabaseOpenHelper.CARS_ID_COLUMN,
                DatabaseOpenHelper.CARS_NAME_COLUMN,
                DatabaseOpenHelper.CARS_BT_MAC_COLUMN,
                DatabaseOpenHelper.CARS_COLOR_COLUMN,
                DatabaseOpenHelper.CARS_LATITUDE_COLUMN,
                DatabaseOpenHelper.CARS_LONGITUDE_COLUMN
        };

        int id_index = 0;
        int name_index = 1;
        int address_index = 2;
        int color_index = 3;
        int latitude_index = 4;
        int longitude_index = 5;

        Cursor cursor = resolver.query(CarContentProvider.carsUri(), projection, null, null, null);

        if( cursor == null )
            return cars;

        try {
            while( cursor.moveToNext() ) {
                cars.add(new Car(cursor, id_index, name_index, address_index, color_index, latitude_index, longitude_index));
            }
        } finally {
            cursor.close();
        }

        return cars;
    }
}
