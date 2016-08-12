package be.ndusart.carfinder.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import be.ndusart.carfinder.R;
import be.ndusart.carfinder.model.Car;

public class SettingsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView noCarWarningLabel = (TextView) findViewById(R.id.no_car_warning_label);

        if( noCarWarningLabel != null ) {
            if (Car.numberOfCars(this) > 0)
                noCarWarningLabel.setVisibility(View.GONE);
            else {
                noCarWarningLabel.setVisibility(View.VISIBLE);
            }
        }
    }
}
