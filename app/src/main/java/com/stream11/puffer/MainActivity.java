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

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    protected ArrayList<LinearLayout> mRows = new ArrayList<LinearLayout>();
    protected TimerTask mTimer;
    protected int mHighligtedRow = 0;

    protected int mSpeed = 60;
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
    private final int MS_IN_MIN = 60000;

    private String mDeviceAddress =  "A4:D5:78:0E:0A:4F";
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private Boolean mIsDebug = false;
    private EditText mEditDebugCommand;
    private Button mButtonSendDebugCommand;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        mEditDebugCommand   = (EditText)findViewById(R.id.editTextDebugCommand);
        mButtonSendDebugCommand = (Button) findViewById(R.id.bnDebugSendCommand);
        mButtonSendDebugCommand.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String cmd = mEditDebugCommand.getText().toString();
                Log.d(TAG, "Debug sending command: " + cmd);

                sendCommandToTower(cmd);
            }
        });

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
        if (id == R.id.action_debug) {
            mIsDebug=true;
            LinearLayout llDebug = (LinearLayout) findViewById(R.id.layoutDebug);
            llDebug.setVisibility(View.VISIBLE);
            LinearLayout llNormal = (LinearLayout) findViewById(R.id.verticalLayout);
            llNormal.setVisibility(View.GONE);
            return true;
        }
        if (id == R.id.action_normal) {
            mIsDebug=false;
            LinearLayout llDebug = (LinearLayout) findViewById(R.id.layoutDebug);
            llDebug.setVisibility(View.GONE);
            LinearLayout llNormal = (LinearLayout) findViewById(R.id.verticalLayout);
            llNormal.setVisibility(View.VISIBLE);
            return true;
        }
        if (id == R.id.action_clear) {
            if (mIsDebug) {
                mEditDebugCommand.setText("");
            } else {
                ceasefire();
            }
            return true;
        }
        if (id == R.id.action_save) {
            savePattern();
        }
        if (id == R.id.action_load) {
            ceasefire();
            createFileSelectorDialog();
        }
        if (id == R.id.action_about) {
            Intent intentAbout = new Intent(this, AboutActivity.class);
            startActivity(intentAbout);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(mIsDebug) {
            menu.findItem(R.id.action_debug).setVisible(false);
            menu.findItem(R.id.action_normal).setVisible(true);
        } else {
            menu.findItem(R.id.action_debug).setVisible(true);
            menu.findItem(R.id.action_normal).setVisible(false);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateSettings();
    }


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
        Log.v(TAG + " connection starte", getResources().getString(resourceId));
    }


    private void ceasefire() {
        for (int rowId = 0; rowId < mNumberOfTimeEvents; rowId++) {
            for (int lineId = 0; lineId < mNumberOfLines; lineId++) {
                int idFire = rowId * mNumberOfLines + lineId;
                Fire fire = (Fire) findViewById(idFire);
                fire.setOn(false);
            }
        }
    }
    public static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private void readPatternFromFile(final String fileName) {
        Log.v(TAG, "Will read pattern from \"" + fileName + "\"");

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;

            int iInputRow = 0;
            while ((line = br.readLine()) != null) {
                Log.v(TAG, "read: " + line);
                String[] rowData = line.split(",");
                for (int i=0; i<rowData.length; i++) {
                    if (i<mNumberOfLines && isNumeric(rowData[i])) {
                        double state = Double.parseDouble(rowData[i]);

                        if (state>0) {
                            int idFire = iInputRow * mNumberOfLines + i;
                            Fire fire = (Fire) findViewById(idFire);
                            fire.setOn(true);
                            Log.v(TAG, "Will set on: " + String.valueOf(idFire));
                        }
                    }
                }
                iInputRow++;
            }
            br.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File read failed: " + e.toString());
        }
    }


    private String[] getFileList() {
        File filePath = getFilesDir();
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File sel = new File(dir, filename);
                return filename.contains(".csv");
            }
        };

        String[] fileList = filePath.list(filter);
        if (fileList==null) {
            fileList = new String[0];
        }
        return fileList;
    }

    protected void createFileSelectorDialog() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("Select One Name:-");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.select_dialog_singlechoice);
        arrayAdapter.addAll(getFileList());

        builderSingle.setNegativeButton(
                "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName =  getFilesDir() + File.separator + arrayAdapter.getItem(which);
                        Log.v(TAG, "Will load data from" + fileName);
                        readPatternFromFile(fileName);
                    }
                });
        builderSingle.show();

    }

    private void savePatternToFile(final String fileName) {
        Log.v(TAG, "Will save the pattern to \"" + fileName + "\"");
        try {OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(fileName + ".csv", Context.MODE_PRIVATE));

            for (int rowId = 0; rowId< mNumberOfTimeEvents; rowId++) {
                for (int lineId = 0; lineId < mNumberOfLines; lineId++) {
                    int idFire = rowId * mNumberOfLines + lineId;
                    Fire fire = (Fire) findViewById(idFire);
                    if (fire!=null) {
                        if (fire.isOn()) {
                            outputStreamWriter.write("1");
                        } else  {
                            outputStreamWriter.write("0");
                        }
                        if (lineId < mNumberOfTimeEvents-1) {
                            outputStreamWriter.write(",");
                        }
                    }
                }
                outputStreamWriter.write("\n");
            }

            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void savePattern() {
        final EditText txtFileName = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle(R.string.save_dialog_title)
                .setMessage(R.string.save_dialog_prompt)
                .setView(txtFileName)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String fileName = txtFileName.getText().toString();
                        savePatternToFile(fileName);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
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
