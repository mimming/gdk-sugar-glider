package com.mimming.sugarglider;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;

public class MapDisplayService extends Service {
    private static final String TAG = "MapDisplayService";

    private WindowManager windowManager;
//    private TextView floatingDate;
    private TextView floatingCoords;
//    BroadcastReceiver broadcastReceiver;


    @Override public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

//    static SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

    @Override public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Creating map service");

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location loc) {
                Log.v(TAG, "Location manager detected a change");
                String s = loc.getLongitude() + ", " + loc.getLatitude();
                floatingCoords.setText(s);
            }

            @Override
            public void onProviderDisabled(String provider) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        List<String> providers = locationManager.getAllProviders();
        for (String provider : providers) {
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, 5000, 10, locationListener);
            }
        }

//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);


        floatingCoords = new TextView(this);
//        floatingCoords.setText("Coords go here");


//        floatingDate = new TextView(this);
//        floatingDate.setText(format.format(new Date()));

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.CENTER;
        params.x = 0;
        params.y = 0;

        windowManager.addView(floatingCoords, params);

//        broadcastReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context ctx, Intent intent)
//            {
//                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
//                    floatingDate.setText(format.format(new Date()));
//                }
//            }
//        };

//        registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Map")
                .getNotification();

        startForeground(123, notification);
    }

    @Override
    public void onDestroy() {

        stopForeground(true);

//        if (broadcastReceiver != null)
//            unregisterReceiver(broadcastReceiver);

        super.onDestroy();
        if (floatingCoords != null) windowManager.removeView(floatingCoords);
    }


}
