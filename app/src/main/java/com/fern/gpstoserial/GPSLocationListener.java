package com.fern.gpstoserial;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class GPSLocationListener implements LocationListener {
    private final String TAG = this.getClass().getName();

    private final LocationManager locationManager;
    private final UsbSerialPort usbSerialPort;
    private final TextView textLat, textLon, textAccuracy;
    private final Context context;

    private final byte[] serialBuffer = new byte[11];

    GPSLocationListener(LocationManager locationManager,
                        UsbSerialPort usbSerialPort,
                        TextView textLat,
                        TextView textLon,
                        TextView textAccuracy,
                        Context context) {
        this.locationManager = locationManager;
        this.usbSerialPort = usbSerialPort;
        this.textLat = textLat;
        this.textLon = textLon;
        this.textAccuracy = textAccuracy;
        this.context = context;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationChanged(Location loc) {
        // Set user messages
        String latitude = "Latitude: " + loc.getLatitude();
        String longitude = "Longitude: " + loc.getLongitude();
        String accuracy = "Accuracy (m): " + loc.getAccuracy();
        textLat.setText(latitude);
        textLon.setText(longitude);
        textAccuracy.setText(accuracy);

        // Convert to Integer
        int latitudeInt = (int) (loc.getLatitude() * 1000000.);
        int longitudeInt = (int) (loc.getLongitude() * 1000000.);

        // Fill serial buffer
        serialBuffer[0] = (byte) ((latitudeInt >> 24) & 0xFF);
        serialBuffer[1] = (byte) ((latitudeInt >> 16) & 0xFF);
        serialBuffer[2] = (byte) ((latitudeInt >> 8) & 0xFF);
        serialBuffer[3] = (byte) (latitudeInt & 0xFF);
        serialBuffer[4] = (byte) ((longitudeInt >> 24) & 0xFF);
        serialBuffer[5] = (byte) ((longitudeInt >> 16) & 0xFF);
        serialBuffer[6] = (byte) ((longitudeInt >> 8) & 0xFF);
        serialBuffer[7] = (byte) (longitudeInt & 0xFF);

        // Packet ending (EEEF)
        serialBuffer[9] = (byte) 0xEE;
        serialBuffer[10] = (byte) 0xEF;

        // Calculate XOR check-sum
        serialBuffer[8] = 0;
        for (int i = 0; i <= 7; i++)
            serialBuffer[8] = (byte) ((serialBuffer[8] ^ serialBuffer[i]) & 0xFF);

        // Send buffer over serial
        try {
            usbSerialPort.write(serialBuffer, 0);
        } catch (Exception e) {
            // Print exception message
            Toast.makeText(context,
                    "Error writing to serial port!",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error writing to serial port!", e);

            // Stop location updates
            locationManager.removeUpdates(this);

            // Reset text fields
            textLat.setText("Latitude: -");
            textLon.setText("Longitude: -");
            textAccuracy.setText("Accuracy (m): -");
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onProviderDisabled(String provider) {
        textLat.setText("Latitude: -");
        textLon.setText("Longitude: -");
        textAccuracy.setText("Accuracy (m): -");
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
