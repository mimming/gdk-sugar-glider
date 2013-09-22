package com.mimming.sugarglider;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class MapDisplayService extends Service {
    private static final String TAG = "MapDisplayService";

    private WindowManager windowManager;
//    private TextView floatingDate;
    private TextView floatingCoords;
    private ImageView floatingMap;
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
                String url = "http://maps.googleapis.com/maps/api/staticmap?center=" +
                        loc.getLatitude() + "," + loc.getLongitude() +
                        "&zoom=17&size=630x350&maptype=roadmap&markers=color:green%7Clabel:G%7C" +
                        loc.getLatitude() + "," + loc.getLongitude() +
                        "&sensor=false&key=AIzaSyB86OWhdiF64GeNbugDTr_xDK3ezrHWlI8" +
                        "&style=feature:road.local%7Celement:geometry%7Ccolor:0x009900%7Cweight:1%7Cvisibility:on&style=feature:landscape%7Celement:geometry.fill%7Ccolor:0x000000%7Cvisibility:on&style=feature:administrative%7Celement:labels%7Cweight:3.9%7Cvisibility:on%7Cinvert_lightness:true&style=feature:poi%7Cvisibility:simplified";
//                String s = loc.getLongitude() + ", " + loc.getLatitude();
//                floatingCoords.setText(url);
//                floatingMap.setImageDrawable(LoadImageFromWebOperations(url));
                new ImageFetcher().execute(url);
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
        floatingMap = new ImageView(this);
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

        windowManager.addView(floatingMap, params);

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

//    private static Drawable LoadImageFromWebOperations(String url) {
//        try {
//            InputStream is = (InputStream) new URL(url).getContent();
//            Drawable d = Drawable.createFromStream(is, "src name");
//            return d;
//        } catch (Exception e) {
//            Log.v(TAG, "Failed to download image");
//            return null;
//        }
//    }

    private class ImageFetcher extends AsyncTask<String, Void, Drawable> {

        private Exception exception;

        @Override
        protected Drawable doInBackground(String... urls) {
            try {
                String url = urls[0];
//                InputStream is = (InputStream) new URL("http://mimming.com/index_assets/headshot_logo.jpg").getContent();
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet request = new HttpGet(url);
                HttpResponse response = httpclient.execute(request);

                InputStream is = response.getEntity().getContent();


//                HttpURLConnection connection = (HttpURLConnection)new URL(urls[0]).openConnection();
//                InputStream is = connection.getInputStream();

                return Drawable.createFromStream(is, "src name");
            } catch (Exception e) {
                Log.v(TAG, "Failed to load image: " + e.getMessage());
                this.exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            Log.v(TAG, "Got image, drawoing map");

            floatingMap.setImageDrawable(drawable);
            floatingMap.setMinimumHeight(360);
            floatingMap.setMinimumWidth(640);
        }
    }

}
