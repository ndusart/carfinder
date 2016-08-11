package be.ndusart.carfinder.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import be.ndusart.carfinder.provider.CarContentProvider;

public class Car {
    public static Boolean hasAnyCar(Context context) {
        ContentResolver resolver = context.getContentResolver();

        // get all cars
        Cursor cursor = resolver.query(CarContentProvider.carsUri(), null, null, null, null);

        Boolean anyCar = cursor.getCount() > 0;
        cursor.close();

        return anyCar;
    }
}
