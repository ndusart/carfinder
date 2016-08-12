package be.ndusart.carfinder.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

import be.ndusart.carfinder.R;
import be.ndusart.carfinder.model.Car;

public class NewCarActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout noBluetoothLayout;
    private Button enableBluetoothButton;

    private TextView noPairedDeviceLabel;

    private LinearLayout formLayout;
    private RelativeLayout selectCarLayout;
    private Spinner selectCarSpinner;
    private Button selectCarButton;
    private RelativeLayout configureCarLayout;
    private EditText carNameEdit;
    private Button configureCarButton;
    private Spinner colorSpinner;

    private BluetoothAdapter btAdapter;

    private ArrayAdapter<String> devicesAdapter;
    private ArrayList<String> pairedAddresses;

    private ArrayAdapter<String> colorsAdapter;
    private ArrayList<Integer> colorCodes;

    private Car newCar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.new_car_dialog);

        noBluetoothLayout = (LinearLayout) findViewById(R.id.no_bluetooth_layout);

        enableBluetoothButton = (Button) findViewById(R.id.enable_bluetooth_button);
        if( enableBluetoothButton != null ) {
            enableBluetoothButton.setOnClickListener(this);
        }

        noPairedDeviceLabel = (TextView) findViewById(R.id.no_paired_device_label);

        formLayout = (LinearLayout) findViewById(R.id.new_car_form_layout);

        selectCarLayout = (RelativeLayout) findViewById(R.id.select_car_form_layout);
        selectCarSpinner = (Spinner) findViewById(R.id.select_car_spinner);
        selectCarButton = (Button) findViewById(R.id.select_device_button);

        pairedAddresses = new ArrayList<>();
        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        devicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if( selectCarSpinner != null ) {
            selectCarSpinner.setAdapter(devicesAdapter);
        }

        if( selectCarButton != null )
            selectCarButton.setOnClickListener(this);

        configureCarLayout = (RelativeLayout) findViewById(R.id.configure_car_form_layout);
        if( configureCarLayout != null )
            configureCarLayout.setVisibility(View.GONE);

        carNameEdit = (EditText) findViewById(R.id.name_edit);
        configureCarButton = (Button) findViewById(R.id.configure_car_button);

        if( configureCarButton != null )
            configureCarButton.setOnClickListener(this);

        colorsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Blue", "Black", "Red"});
        colorsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorCodes = new ArrayList<>();
        colorCodes.add(Color.BLUE);
        colorCodes.add(Color.BLACK);
        colorCodes.add(Color.RED);
        colorSpinner = (Spinner) findViewById(R.id.color_spinner);
        if( colorSpinner != null )
            colorSpinner.setAdapter(colorsAdapter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateDevicesAdapter();

        final int SHOW_NO_BLUETOOTH = 1;
        final int SHOW_NO_PAIRED_DEVICES = 2;
        final int SHOW_FORM = 3;
        int show;

        if (btAdapter == null || !btAdapter.isEnabled())
            show = SHOW_NO_BLUETOOTH;
        else if( ! hasPairedDeviceFree() )
            show = SHOW_NO_PAIRED_DEVICES;
        else
            show = SHOW_FORM;

        if( noBluetoothLayout != null ) {
            if (show == SHOW_NO_BLUETOOTH) {
                noBluetoothLayout.setVisibility(View.VISIBLE);
            } else {
                noBluetoothLayout.setVisibility(View.GONE);
            }
        }

        if( noPairedDeviceLabel != null ) {
            if( show == SHOW_NO_PAIRED_DEVICES )
                noPairedDeviceLabel.setVisibility(View.VISIBLE);
            else
                noPairedDeviceLabel.setVisibility(View.GONE);
        }

        if( formLayout != null ) {
            if( show == SHOW_FORM )
                formLayout.setVisibility(View.VISIBLE);
            else
                formLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if( view == enableBluetoothButton ) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        } else if( view == selectCarButton ) {
            int position = selectCarSpinner.getSelectedItemPosition();
            final String name = devicesAdapter.getItem(position);
            String address = pairedAddresses.get(position);

            newCar = new Car(name, address, Color.BLUE, null);

            carNameEdit.setText(name);
            selectCarLayout.setVisibility(View.GONE);
            configureCarLayout.setVisibility(View.VISIBLE);
        } else if( view == configureCarButton ) {
            try {
                newCar.setName(carNameEdit.getText().toString());
                newCar.setColor(colorCodes.get(colorSpinner.getSelectedItemPosition()));
                newCar.save(this);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finish();
            }
        }
    }

    private void updateDevicesAdapter() {
        devicesAdapter.clear();

        ArrayList<String> names = new ArrayList<>();
        pairedAddresses.clear();

        Set<BluetoothDevice> paired = btAdapter.getBondedDevices();
        for ( BluetoothDevice device : paired ) {
            names.add(device.getName());
            pairedAddresses.add(device.getAddress());
        }

        devicesAdapter.addAll(names);
        devicesAdapter.notifyDataSetChanged();
    }

    private boolean hasPairedDeviceFree() {
        // this check if there are paired devices that are not yet considered as car

        if(btAdapter == null || !btAdapter.isEnabled())
            return false;

        ArrayList<Car> cars = Car.getAllCars(this);
        Set<BluetoothDevice> paired = btAdapter.getBondedDevices();

        if( paired.size() == 0 )
            return false;

        if( cars.size() == 0 )
            return true;

        for ( Car car : cars ) {
            String address = car.getMacAddress();

            boolean ok = true;
            for (BluetoothDevice device : paired) {
                if (device.getAddress().equals(address)) {
                    ok = false;
                }
            }

            if( ok )
                return true;
        }

        return false;
    }
}
