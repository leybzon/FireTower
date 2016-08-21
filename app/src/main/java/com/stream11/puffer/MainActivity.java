package com.stream11.puffer;

/*
 *       Copyright 2016 Gene Leybzon
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */

import android.animation.ValueAnimator;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    final int mNumberOfLines = 22;
    final int mNumberOfTimeEvents = 40;

    int mControlSize = 20;

    protected Matrix mMatrix = new Matrix();
    protected ValueAnimator mAnimator;
    protected TimeLineView mTimeLineView;

    protected ArrayList<LinearLayout> mRows = new ArrayList<LinearLayout>();
    protected TimerTask mTimer;
    protected int mHighligtedRow = 0;

    protected int mSpeed = 60;
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
    private final int MS_IN_MIN = 60000;


    //private String mDeviceName;
    private String mDeviceAddress =  "A4:D5:78:0E:0A:4F";//new String(R.string.default_address); //"A4:D5:78:0E:0A:4F";
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Fire testFire = new Fire(this);
        //testFire.setMinimumWidth(10);
        //testFire.setMinimumHeight(10);
        //testFire.setImageResource(R.drawable.ic_launcher);


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        mControlSize = Math.min((width - 6) / mNumberOfLines, (height - 4) / mNumberOfTimeEvents); //double the size


        LinearLayout ll = (LinearLayout) findViewById(R.id.verticalLayout);


        for (int i = 0; i < mNumberOfTimeEvents; i++) {
            LinearLayout row = addRow(ll, i);
            mRows.add(i, row);
        }

        updateSettings();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    protected void updateSettings() {
        SharedPreferences settings = getSharedPreferences(SettingsActivity.SETTINGS, MODE_PRIVATE);

        mSpeed = settings.getInt(SettingsActivity.SPEED, mSpeed);
        if (mSpeed <= 0) {
            mSpeed = 1;
        }
        startInfiniteTimer(MS_IN_MIN / mSpeed);
    }

    protected void startInfiniteTimer(int intervalMs) {
        Log.w(this.toString(), "New timer interval: " + String.valueOf(intervalMs));

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        mTimer = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout row = mRows.get(mHighligtedRow);
                        row.setBackgroundColor(Color.TRANSPARENT);
                        mHighligtedRow = (mHighligtedRow + 1) % mNumberOfTimeEvents;
                        LinearLayout row2 = mRows.get(mHighligtedRow);
                        row2.setBackgroundColor(Color.BLUE);
                    }
                });

                String cmd = getFiresCmd(mHighligtedRow);
                Log.v(this.toString(), cmd);
                sendCommandToTower(cmd);
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(mTimer, intervalMs, intervalMs);
    }

    protected String getFiresCmd(int rowId) {
        StringBuffer cmd = new StringBuffer();
        cmd.append((int) (60000 / mSpeed));  //fire time in [ms]
        cmd.append(',');

        for (int lineId = 0; lineId < mNumberOfLines; lineId++) {
            int id = rowId * mNumberOfLines + lineId;
            Fire fire = (Fire) findViewById(id);
            if (fire.isOn()) {
                cmd.append(lineId);
                cmd.append(',');
            }
        }
        String ret = cmd.toString();
        ret = ret.substring(0, ret.length() - 1);

        return ret;
    }

    protected LinearLayout addRow(LinearLayout parent, int rowId) {
        LinearLayout LL = new LinearLayout(this);
        LL.setId(10000 + rowId);

        //LL.setBackgroundColor(Color.CYAN);
        LL.setOrientation(LinearLayout.HORIZONTAL);

        LayoutParams LLParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        LL.setWeightSum(6f);
        LL.setLayoutParams(LLParams);


        for (int i = 0; i < mNumberOfLines; i++) {
            Fire fire = new Fire(this);
            fire.setLayoutParams(new LayoutParams(mControlSize, mControlSize));
            fire.setImageResource(R.mipmap.ic_launcher);
            fire.setId(rowId * mNumberOfLines + i);
            LL.addView(fire);
        }

        parent.addView(LL);
        return LL;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, 0);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateSettings();
    }

    //-----------------------------
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
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void sendCommandToTower(String s) {
        s += '\n';

        if (bluetoothGattCharacteristicHM_10 != null) {
            byte[] rxBytes = bluetoothGattCharacteristicHM_10.getValue();


            //final byte[] insertSomething = {(byte)'A', (byte)'B', (byte)'C'};
            final byte[] insertSomething = s.getBytes(Charset.forName("UTF-8"));

            if (rxBytes == null) {
                rxBytes = insertSomething;
            }


            byte[] txBytes = new byte[insertSomething.length + rxBytes.length];
            System.arraycopy(insertSomething, 0, txBytes, 0, insertSomething.length);
            System.arraycopy(rxBytes, 0, txBytes, insertSomething.length, rxBytes.length);

            bluetoothGattCharacteristicHM_10.setValue(txBytes);
            mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
            mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10, true);
        } else {
            Log.e(TAG, "Not connected to HM-10 tower (" + s + ")");
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

                sendCommandToTower("Hello!");

                Toast toast = Toast.makeText(getApplicationContext(), R.string.connected, Toast.LENGTH_SHORT);
                toast.show();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
                Toast toast = Toast.makeText(getApplicationContext(), R.string.disconnected, Toast.LENGTH_SHORT);
                toast.show();
                
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                Log.v(TAG, intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                if (bluetoothGattCharacteristicHM_10 != null) {
                    //Echo back received data, with something inserted  //toDO~

                    final byte[] rxBytes = bluetoothGattCharacteristicHM_10.getValue();
                    final byte[] insertSomething = {(byte) 'A'};
                    byte[] txBytes = new byte[insertSomething.length + rxBytes.length];
                    System.arraycopy(insertSomething, 0, txBytes, 0, insertSomething.length);
                    System.arraycopy(rxBytes, 0, txBytes, insertSomething.length, rxBytes.length);


                    bluetoothGattCharacteristicHM_10.setValue(txBytes);
                    mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
                    mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10, true);
                }

            }
        }
    };


    private void displayGattServices(List<BluetoothGattService> gattServices) {

        UUID UUID_HM_10 =
                UUID.fromString(SampleGattAttributes.HM_10);

        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                //Check if it is "HM_10"
                if (uuid.equals(SampleGattAttributes.HM_10)) {
                    bluetoothGattCharacteristicHM_10 = gattService.getCharacteristic(UUID_HM_10);

                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }


    private void updateConnectionState(final int resourceId) {
     /*   runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
*/  //TODO!

        Log.v(TAG + " connection starte", getResources().getString(resourceId));
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        //if (mTimer != null) {
        //    mTimer.cancel();
        //}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        if (mTimer != null) {
            mTimer.cancel();
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
