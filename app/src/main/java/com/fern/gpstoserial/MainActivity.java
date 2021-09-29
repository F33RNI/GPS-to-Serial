/*
 * Copyright (C) 2021 Fern H. (Pavel Neshumov), GPS to Serial Android application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.fern.gpstoserial;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int PERMISSION_REQUEST_CODE = 1;

    private LocationManager locationManager;

    private GPSLocationListener gpsLocationListener;

    private UsbSerialPort usbSerialPort;
    private EditText editBaudRate;
    private TextView textLat, textLon, textAccuracy;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Screen parameters
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Define containers
        editBaudRate = findViewById(R.id.editBaudRate);
        textLat = findViewById(R.id.textLat);
        textLon = findViewById(R.id.textLon);
        textAccuracy = findViewById(R.id.textAccuracy);

        // Connect start button
        findViewById(R.id.btnConnect).setOnClickListener(view -> startGPStoSerial());

        // Connect stop button
        findViewById(R.id.btnStop).setOnClickListener(view -> stopGPStoSerial());
    }

    /**
     * Opens serial port and start GPS updates
     */
    private void startGPStoSerial() {
        if (locationManager == null || gpsLocationListener == null || usbSerialPort == null) {
            // Reset variables
            locationManager = null;
            gpsLocationListener = null;
            usbSerialPort = null;

            // Find all available drivers from attached devices
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            List<UsbSerialDriver> availableDrivers =
                    UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                Toast.makeText(getApplicationContext(), "No serial USB devices!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Open a connection to the first available driver
            UsbSerialDriver driver;
            try {
                driver = availableDrivers.get(0);
            } catch (Exception ignored) {
                Toast.makeText(getApplicationContext(), "No correct serial USB devices!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Check and grant permissions
            if (!checkAndRequestPermission(manager, driver.getDevice())) {
                Toast.makeText(getApplicationContext(),
                        "Please grant permission and try again", Toast.LENGTH_LONG).show();
                return;
            }

            // Open USB device
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                Toast.makeText(getApplicationContext(), "Error opening USB device!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Open first available serial port
                usbSerialPort = driver.getPorts().get(0);
                usbSerialPort.open(connection);
                usbSerialPort.setParameters(Integer.parseInt(editBaudRate.getText().toString()),
                        8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                // Request GPS updates using locationCriteria
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                gpsLocationListener =
                        new GPSLocationListener(locationManager,
                                usbSerialPort, textLat, textLon, textAccuracy,
                                getApplicationContext());
                Criteria locationCriteria = new Criteria();
                locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
                locationManager.requestLocationUpdates(
                        locationManager.getBestProvider(locationCriteria, true),
                        1000, 0, gpsLocationListener);

                // Print message
                Toast.makeText(getApplicationContext(),
                        "GPS reading has started", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // Print exception message
                Toast.makeText(getApplicationContext(),
                        "Error starting GPS listener or opening serial port!",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error starting GPS listener or opening serial port!", e);
            }
        }
    }

    /**
     * Stops GPS updates and closes serial port
     */
    @SuppressLint("SetTextI18n")
    private void stopGPStoSerial() {
        try {
            if (locationManager != null && gpsLocationListener != null && usbSerialPort != null) {
                // Stop GPS readings
                locationManager.removeUpdates(gpsLocationListener);

                // Close serial port
                usbSerialPort.close();

                // Set default text
                textLat.setText("Latitude: -");
                textLon.setText("Longitude: -");
                textAccuracy.setText("Accuracy (m): -");

                // Print message
                Toast.makeText(getApplicationContext(),
                        "GPS reading has stopped", Toast.LENGTH_LONG).show();

                // Reset variables
                locationManager = null;
                gpsLocationListener = null;
                usbSerialPort = null;
            }
        } catch (Exception e) {
            // Print exception message
            Toast.makeText(getApplicationContext(),
                    "Error stopping GPS listener or closing serial port!",
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error stopping GPS listener or closing serial port!", e);
        }
    }

    /**
     * Checks for access permissions to the USB device and GPS location.
     * If there are no permissions, asks for it.
     * @param manager UsbManager class
     * @param usbDevice USB device to be accessed
     * @return true if permissions already exist, permissions if rights are requested
     */
    private boolean checkAndRequestPermission(UsbManager manager, UsbDevice usbDevice) {
        // Check if permissions already exists
        if (hasPermissions(getApplicationContext(), PERMISSIONS)
                && manager.hasPermission(usbDevice))
            return true;
        else {
            // Request GPS permissions
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS,
                    PERMISSION_REQUEST_CODE);

            // Request USB permission
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                    0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(usbDevice, pendingIntent);
            return false;
        }
    }

    /**
     * Checks for permissions
     * @param context Activity
     * @param permissions List of permissions
     * @return true if all permissions were granted
     */
    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}