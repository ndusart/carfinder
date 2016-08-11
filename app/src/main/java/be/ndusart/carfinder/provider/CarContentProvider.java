package be.ndusart.carfinder.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import be.ndusart.carfinder.model.DatabaseOpenHelper;

public class CarContentProvider extends ContentProvider {

    private DatabaseOpenHelper mDatabase;

    public static final String AUTHORITY = "be.ndusart.carfinderapp.carprovider";
    public static final String BASE_CONTENT_URI = "content://" + AUTHORITY;

    //////////
    // URIS //
    //////////

    // /cars -> get all cars
    public static final int URI_CARS = 1;

    // /cars/{ID} -> get car with given id
    public static final int URI_CAR = 2;

    // URIS end

    private static final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        mUriMatcher.addURI(AUTHORITY, "cars", URI_CARS);
        mUriMatcher.addURI(AUTHORITY, "cars/#", URI_CAR);
    }

    @Override
    public boolean onCreate() {
        mDatabase = new DatabaseOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        final int match = mUriMatcher.match(uri);

        SQLiteDatabase db = mDatabase.getReadableDatabase();

        switch(match) {
            case URI_CAR:
                qb.appendWhere(DatabaseOpenHelper.CARS_ID_COLUMN + " = " + uri.getLastPathSegment());
            case URI_CARS:
                qb.setTables(DatabaseOpenHelper.CARS_TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Unkown uri: " + uri);
        }

        if( cursor == null ) {
            cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Cars provider does not provide MIME types");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final int match = mUriMatcher.match(uri);
        String table = "";

        Uri newUri = null;

        switch(match){
            case URI_CAR:
                values.put(DatabaseOpenHelper.CARS_ID_COLUMN, uri.getLastPathSegment());
            case URI_CARS:
                table = DatabaseOpenHelper.CARS_TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unkown uri: " + uri);
        }

        if( newUri == null && table.length() > 0 ) {
            SQLiteDatabase db = mDatabase.getWritableDatabase();

            long newId = db.insert(table, null, values);

            if( newId >= 0 ) {
                newUri = Uri.parse(BASE_CONTENT_URI+"/"+table+"/"+newId);

                getContext().getContentResolver().notifyChange(newUri, null);
            } else {
                throw new SQLException("Failed to insert row into uri: " + uri);
            }
        } else {
            // presume the insertion has been done in the switch clause
            getContext().getContentResolver().notifyChange(newUri, null);
        }

        return newUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final int match = mUriMatcher.match(uri);
        String table = null;
        int count = 0;

        if( selection == null )
            selection = "";

        switch(match){
            case URI_CAR:
                selection = appendToSelection(selection, DatabaseOpenHelper.CARS_ID_COLUMN + " = " + uri.getLastPathSegment());
            case URI_CARS:
                table = DatabaseOpenHelper.CARS_TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unkown uri: " + uri);
        }

        if( table != null ) {
            SQLiteDatabase db = mDatabase.getWritableDatabase();
            count = db.delete(table, selection, selectionArgs);

            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = mUriMatcher.match(uri);
        String table = null;
        int count = 0;

        if( selection == null )
            selection = "";

        switch(match){
            case URI_CAR:
                selection = appendToSelection(selection, DatabaseOpenHelper.CARS_ID_COLUMN + " = " + uri.getLastPathSegment());
            case URI_CARS:
                table = DatabaseOpenHelper.CARS_TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unkown uri: " + uri);
        }

        if( table != null ) {
            SQLiteDatabase db = mDatabase.getWritableDatabase();
            count = db.update(table, contentValues ,selection, selectionArgs);

            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    static public Uri carsUri() {
        return Uri.parse(BASE_CONTENT_URI+"/cars");
    }

    static public Uri carUri(long id) {
        return Uri.parse(BASE_CONTENT_URI+"/cars/"+id);
    }

    private String appendToSelection(String start, String append) {
        String newSelection = start;
        if( newSelection == null || newSelection.length() == 0 ) {
            newSelection = append;
        } else {
            newSelection = newSelection + " AND (" + append + ")";
        }
        return newSelection;
    }
}
