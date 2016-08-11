package be.ndusart.carfinder.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DatabaseOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "carfinder_db";
    private static final int DATABASE_VERSION = 1;

    public static final String CARS_TABLE_NAME = "cars";
    public static final String CARS_ID_COLUMN = BaseColumns._ID;
    public static final String CARS_NAME_COLUMN = "name";
    public static final String CARS_BT_MAC_COLUMN = "mac";
    public static final String CARS_LATITUDE_COLUMN = "latitude";
    public static final String CARS_LONGITUDE_COLUMN = "longitude";
    public static final String CARS_COLOR_COLUMN = "color";

    private static final String CREATE_CARS_TABLE = "CREATE TABLE " + CARS_TABLE_NAME + " (" +
                                                    CARS_ID_COLUMN + " INTEGER PRIMARY KEY, " +
                                                    CARS_NAME_COLUMN + " TEXT, " +
                                                    CARS_BT_MAC_COLUMN + " TEXT, " +
                                                    CARS_LATITUDE_COLUMN + " REAL, " +
                                                    CARS_LONGITUDE_COLUMN + " REAL, " +
                                                    CARS_COLOR_COLUMN + " INTEGER)";

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CARS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
