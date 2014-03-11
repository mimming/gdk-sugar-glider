/*
 * Copyright (C) 2014 Google Inc.
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
 */

package com.mimming.sugarglider;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

import java.util.List;

public class MapDisplayService extends Service {
    private static final String TAG = MapDisplayService.class.getSimpleName();

    public static final String ZOOM_LEVEL_PREF_KEY = "zoom_level";
    public static final int MIN_LOC_UPDATE_MS = 2000;
    public static final int MIN_MAP_UPDATE_MOVEMENT_DISTANCE = 2;
    public static final int DEF_ZOOM_LEVEL = 17;
    private static final String LIVE_CARD_TAG = "map_card";

    private SharedPreferences mSettings;
    private LocationManager mLocationManager;
    private Location mLastKnownLocation;
    private MapImageManager mMapImageManager;
    private LiveCard mLiveCard;
    private RemoteViews mRemoteViews;
    private int mZoom;

    private final MapDisplayBinder mMapDisplayBinder = new MapDisplayBinder();
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Don't update unless we've moved a sensible distance
            if (mLastKnownLocation != null &&
                    mLastKnownLocation.distanceTo(location) < MIN_MAP_UPDATE_MOVEMENT_DISTANCE) {
                return;
            }
            mLastKnownLocation = location;

            mMapImageManager.getMap(location, mZoom, new MapImageManager.ImageFoundCallback() {
                @Override
                public void callback(Bitmap mapBitmap) {
                    updateMap(mapBitmap);
                }
            });
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public class MapDisplayBinder extends Binder {
        public int getZoom() {
            return mZoom;
        }

        public void setZoom(int zoom) {
            mZoom = zoom;
            mMapImageManager.getMap(mLastKnownLocation, mZoom, new MapImageManager.ImageFoundCallback() {
                @Override
                public void callback(Bitmap mapBitmap) {
                    updateMap(mapBitmap);
                }
            });
            mSettings.edit().putInt(ZOOM_LEVEL_PREF_KEY, zoom).commit();
        }

        public Location getLastKnownLocation() {
            return mLastKnownLocation;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMapDisplayBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Publish the card right away with a place holder
        //TODO: replace with a better loading indicator, once an API is available
        RemoteViews placeholderRemoteViews = new RemoteViews(getPackageName(), R.layout.loading_live_card);
        publishCard(this, placeholderRemoteViews);

        // Get the zoom level from a preference
        mSettings = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        mZoom = mSettings.getInt(ZOOM_LEVEL_PREF_KEY, DEF_ZOOM_LEVEL);

        mMapImageManager = MapImageManager.getInstance();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);

        List<String> providers = mLocationManager.getProviders(criteria, true /* enabledOnly */);

        if (providers == null || providers.size() < 1) {
            // TODO: display an error live card if there are no location providers
            Log.e(TAG, "No GPS provider found :(");
            stopSelf();
        } else {
            for (String provider : providers) {
                mLocationManager.requestLocationUpdates(provider, MIN_LOC_UPDATE_MS, 0, mLocationListener);
            }
        }
    }

    @Override
    public void onDestroy() {
        mLocationManager.removeUpdates(mLocationListener);
        unpublishCard();
        super.onDestroy();
    }

    private void updateMap(Bitmap mapBitmap) {
        if(mRemoteViews == null) {
            mRemoteViews = new RemoteViews(getPackageName(), R.layout.map_live_card);
        }
        mRemoteViews.setImageViewBitmap(R.id.map, mapBitmap);

        mLiveCard.setViews(mRemoteViews);
    }

    private void publishCard(Context context, RemoteViews remoteViews) {
        if (mLiveCard == null) {
            TimelineManager tm = TimelineManager.from(context);
            mLiveCard = tm.createLiveCard(LIVE_CARD_TAG);
            mLiveCard.setViews(remoteViews);

            Intent intent = new Intent(context, MenuActivity.class);
            mLiveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));

            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
        }
    }

    private void unpublishCard() {
        if (mLiveCard != null) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
    }
}
