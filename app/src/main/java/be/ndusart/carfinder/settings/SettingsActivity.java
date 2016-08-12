package be.ndusart.carfinder.settings;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import be.ndusart.carfinder.R;
import be.ndusart.carfinder.model.Car;
import be.ndusart.carfinder.model.DatabaseOpenHelper;
import be.ndusart.carfinder.provider.CarContentProvider;

public class SettingsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final int CARS_LOADER_ID = 1;
    private CursorAdapter mAdapter;
    private Loader<Cursor> mLoader;

    private Button addCarButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView noCarWarningLabel = (TextView) findViewById(R.id.no_car_warning_label);
        if (noCarWarningLabel != null) {
            if (Car.numberOfCars(this) > 0)
                noCarWarningLabel.setVisibility(View.GONE);
            else {
                noCarWarningLabel.setVisibility(View.VISIBLE);
            }
        }

        ListView carsList = (ListView) findViewById(R.id.car_list);
        if (carsList != null) {
            mAdapter = new SimpleCursorAdapter(this, R.layout.car_list_item, null, new String[]{DatabaseOpenHelper.CARS_NAME_COLUMN, DatabaseOpenHelper.CARS_BT_MAC_COLUMN}, new int[]{R.id.name_label, R.id.mac_address_label}, 0);
            carsList.setAdapter(mAdapter);

            getSupportLoaderManager().initLoader(CARS_LOADER_ID, null, this);
        }

        addCarButton = (Button) findViewById(R.id.add_car_button);
        if( addCarButton != null ) {
            addCarButton.setOnClickListener(this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if( id == CARS_LOADER_ID ) {
            String[] projection = new String[]{
                    DatabaseOpenHelper.CARS_ID_COLUMN,
                    DatabaseOpenHelper.CARS_NAME_COLUMN,
                    DatabaseOpenHelper.CARS_BT_MAC_COLUMN
            };

            mLoader = new CursorLoader(this, CarContentProvider.carsUri(), projection, null, null, null);
            return mLoader;
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        if( loader == mLoader )
            mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        if( loader == mLoader )
            mAdapter.swapCursor(null);
    }

    @Override
    public void onClick(View view) {
        if( view == addCarButton ) {
            Car car = new Car("Car", "BT", Color.BLUE, 50.03, 4.23);
            try {
                car.save(this);
            } catch (Exception e) {
            }
        }
    }
}
