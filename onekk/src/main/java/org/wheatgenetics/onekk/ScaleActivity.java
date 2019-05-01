package org.wheatgenetics.onekk;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScaleActivity extends AppCompatActivity {

    private final static String TAG = ScaleActivity.class.getSimpleName();
    //public static final int BLUETOOTH_DEVICE_REQUEST = 20;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private BluetoothAdapter mBluetoothAdapter;

    private TextView uiConnectionState;
    private TextView uiDataField;
    private TextView uiDeviceName;
    private TextView uiDeviceAddress;
    private ListView uiBluetoothList;
    private Button uiStopSearchButton;

    private ArrayList<String> mDeviceList;
    ArrayAdapter<String> mListViewAdapter;

    private HashMap<String, BluetoothDevice> mBluetoothMap;

    Intent intent;
    String mDeviceName;
    String mDeviceAddress;
    String mConnected;
    String mWeightData;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scale_activity);

        intent = getIntent();

        Toolbar toolbar;
        toolbar = (Toolbar) findViewById(R.id.scale_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Not Support LE Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }*/

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not support", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uiDeviceName = (TextView) findViewById(R.id.device_name);
        uiDeviceAddress = (TextView) findViewById(R.id.device_address);
        uiConnectionState = (TextView) findViewById(R.id.connection_state);
        uiDataField = (TextView) findViewById(R.id.data_value);
        uiBluetoothList = (ListView) findViewById(R.id.bluetoothList);
        uiStopSearchButton = (Button) findViewById(R.id.stopSearch);
        mDeviceList = new ArrayList<>();
        mListViewAdapter = new ArrayAdapter<>(ScaleActivity.this, android.R.layout.simple_list_item_1, mDeviceList);
        uiBluetoothList.setAdapter(mListViewAdapter);
        mBluetoothMap = new HashMap<>();
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        initUIData();

        uiBluetoothList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String name = adapterView.getItemAtPosition(i).toString();
                Intent request = new Intent();
                request.putExtra("BluetoothDevice", mBluetoothMap.get(name));
                setResult(RESULT_OK, request);
                finish();
            }
        });

        uiStopSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        });

    }

    private void initUIData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceName = intent.getStringExtra("DeviceName");
                mDeviceAddress = intent.getStringExtra("DeviceAddress");
                mConnected = intent.getStringExtra("ConnectionState");
                mWeightData = intent.getStringExtra("WeightData");
                uiDeviceName.setText(mDeviceName);
                uiDeviceAddress.setText(mDeviceAddress);
                uiConnectionState.setText(mConnected);
                uiDataField.setText(mWeightData);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("error", "=========search devices=========");
                    if (device != null && device.getName() != null) {
                        String name = device.getName();
                        if (!mBluetoothMap.containsKey(name)) {
                            mBluetoothMap.put(name, device);
                            mDeviceList.add(name);
                            mListViewAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

}
