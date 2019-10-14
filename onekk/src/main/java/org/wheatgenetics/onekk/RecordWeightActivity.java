package org.wheatgenetics.onekk;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RecordWeightActivity extends AppCompatActivity {

    private final static String TAG = RecordWeightActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothLeService mBluetoothLeService;

    private TextView mConnectionState;
    private TextView mDataField;
    private Button mRecordButton;

    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new MyRunnable();
    private final long mDelayTime = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_weight);

        Toolbar toolbar;
        toolbar = (Toolbar) findViewById(R.id.record_weight_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Not Support LE Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not support", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mRecordButton = (Button) findViewById(R.id.record);
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        //send weight data to main activity
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("Weight", weightData);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device != null) {
                                String deviceName = device.getName();
                                Log.i(TAG, "=====device name=====" + deviceName);
                                if (deviceName != null && deviceName.toLowerCase().startsWith("ohbt")) {
                                    Log.i(TAG, "Find device name start with ohbt");
                                    mBluetoothDevice = device;
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    ((TextView) findViewById(R.id.device_name)).setText(device.getName());
                                    ((TextView) findViewById(R.id.device_address)).setText(device.getAddress());

                                    Intent gattServiceIntent = new Intent(RecordWeightActivity.this, BluetoothLeService.class);
                                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                                }
                            }
                        }
                    });
                }
            };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mBluetoothDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                mConnected = true;
                updateConnectionState("Connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState("Disconnected");
                mDataField.setText("No data");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                try {
                    Thread.currentThread();
                    Thread.sleep(mDelayTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mNotifyCharacteristic = mBluetoothLeService.enableNotifications(BluetoothLeService.UUID_CHARACTERISTIC, true);

                try {
                    Thread.currentThread();
                    Thread.sleep(mDelayTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mNotifyCharacteristic != null) {
                    mNotifyCharacteristic.setValue("0P\r\nI2\r\nPM\r\nHD11\r\n");
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic);

                    try {
                        Thread.currentThread();
                        Thread.sleep(mDelayTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHandler.postDelayed(mRunnable, mDelayTime);
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

            }
        }
    };

    private void updateConnectionState(final String resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mBluetoothDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mHandler.removeCallbacks(mRunnable);
        mBluetoothLeService = null;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private StringBuffer weightDataBuffer = new StringBuffer(256);
    private int weightDataIndex;
    private String weightUnit;
    private int spaceIndex;
    //private String weightPackage;
    private String weightData;

    private void displayData(String data) {
        if (data != null) {
            this.weightDataBuffer.append(data);
            this.weightDataIndex = this.weightDataBuffer.indexOf("\r\n");
            Log.d(TAG, "Scout:" + this.weightDataBuffer + this.weightDataBuffer.length());
            if (this.weightDataIndex >= 2) {
                if (this.weightDataIndex == 24 && this.weightDataBuffer.charAt(0) == '\u0002') {
                    this.weightData = this.weightDataBuffer.substring(this.weightDataIndex - 23, this.weightDataIndex - 11);
                    this.weightUnit = this.weightDataBuffer.substring(this.weightDataIndex - 11, this.weightDataIndex - 6);
                    this.spaceIndex = this.weightData.indexOf(32);
                    while (this.spaceIndex != -1) {
                        if (this.spaceIndex == 0) {
                            this.weightData = this.weightData.substring(1, this.weightData.length());
                        } else {
                            this.weightData = this.weightData.substring(0, this.weightData.length() - 1);
                        }
                        this.spaceIndex = this.weightData.indexOf(32);
                    }
                    this.mDataField.setText(this.weightData);
                    this.spaceIndex = this.weightUnit.indexOf(32);
                    while (this.spaceIndex != -1) {
                        if (this.spaceIndex == 0) {
                            this.weightUnit = this.weightUnit.substring(1, this.weightUnit.length());
                        } else {
                            this.weightUnit = this.weightUnit.substring(0, this.weightUnit.length() - 1);
                        }
                        this.spaceIndex = this.weightData.indexOf(32);
                    }
                    /*this.mUnitField.setText(this.weightUnit);
                    this.weightPackage = this.weightDataBuffer.substring(this.weightDataIndex - 22, this.weightDataIndex - 1);
                    this.errorStrIndex = this.weightDataBuffer.indexOf("?");
                    if (this.errorStrIndex >= 0) {
                        this.mStable.setVisibility(4);
                        this.bLatestStableState = false;
                    } else {
                        this.mStable.setVisibility(0);
                        if (!(this.outStreamOhaus == null || this.bLatestStableState)) {
                            try {
                                this.outStreamOhaus.write(this.weightDataBuffer.toString().getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        this.bLatestStableState = true;
                    }
                    this.errorStrIndex = this.weightDataBuffer.indexOf("Z");
                    if (this.errorStrIndex > 0) {
                        this.mZeroPointMark.setText(">0<");
                    } else {
                        this.mZeroPointMark.setText("");
                    }
                    if (this.weightPackage.indexOf("N") >= 0) {
                        this.mNet.setText("NET");
                    } else if (this.weightPackage.indexOf("PT") >= 0) {
                        this.mNet.setText("PT");
                    } else {
                        this.mNet.setText("");
                    }
                } else {
                    if (this.weightDataBuffer.indexOf("Err 8.3") >= 0) {
                        this.mDataField.setText("Err 8.3");
                        this.mStable.setVisibility(4);
                        this.mZeroPointMark.setText("");
                    }
                    if (this.weightDataBuffer.indexOf("Err 8.4") >= 0) {
                        this.mDataField.setText("Err 8.4");
                        this.mStable.setVisibility(4);
                        this.mZeroPointMark.setText("");
                    }
                    if (this.weightDataBuffer.indexOf("ES") >= 0) {
                        this.mStable.setVisibility(4);
                        this.mZeroPointMark.setText("");
                        this.mUnitField.setText("");
                    }*/
                    if (this.weightDataBuffer.indexOf("I2 A ") >= 0) {
                        if (this.weightDataBuffer.length() > 17) {
                            this.weightData = this.weightDataBuffer.substring(this.weightDataBuffer.indexOf("\"") + 1, this.weightDataBuffer.length());
                            this.weightData = this.weightData.substring(0, this.weightData.indexOf(" "));
                            //this.mScaleModel.setText(this.weightData);
                            //this.mScaleModel.setTextColor(-7829368);
                            if (this.mNotifyCharacteristic != null) {
                                this.mNotifyCharacteristic.setValue("I4\r\n");
                                this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                                if (this.weightDataIndex >= 0) {
                                    this.weightDataBuffer.setLength(0);
                                    return;
                                }
                                return;
                            }
                        } else if (this.mNotifyCharacteristic != null) {
                            this.mNotifyCharacteristic.setValue("I2\r\n");
                            this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                        }
                    }
                    if (this.weightDataBuffer.indexOf("I4 A ") >= 0) {
                        if (this.weightDataBuffer.length() > 9) {
                            this.weightData = this.weightDataBuffer.substring(this.weightDataBuffer.indexOf("\"") + 1, this.weightDataBuffer.length());
                            this.weightData = this.weightData.substring(0, this.weightData.indexOf("\""));
                            //this.mScaleSN.setTextColor(-7829368);
                            //this.mScaleSN.setText(this.weightData);
                        } else if (this.mNotifyCharacteristic != null) {
                            this.mNotifyCharacteristic.setValue("I4\r\n");
                            this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                        }
                    }
                }
                if (this.mNotifyCharacteristic != null) {
                    this.mNotifyCharacteristic.setValue("BP\r\n");
                    this.mBluetoothLeService.writeCharacteristic(this.mNotifyCharacteristic);
                }
                this.mHandler.removeCallbacks(this.mRunnable);
                this.mHandler.postDelayed(this.mRunnable, mDelayTime);
            }
            if (this.weightDataIndex >= 0) {
                this.weightDataBuffer.setLength(0);
            }
        }
    }

    class MyRunnable implements Runnable {

        public void run() {
            if (mNotifyCharacteristic != null) {
                mNotifyCharacteristic.setValue("HD11\r\nPM\r\n");
                mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic);
            }
            mHandler.postDelayed(this, mDelayTime);
        }
    }
}
