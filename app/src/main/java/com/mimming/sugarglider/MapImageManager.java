/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mimming.sugarglider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;

/**
 * Fetches and caches images of maps.
 */
public class MapImageManager {
    private static final String TAG = MapImageManager.class.getSimpleName();

    public enum MapType {
        STANDARD, HIGH_CONTRAST, SATELLITE, TERRAIN;

        private String URL;

        static {
            STANDARD.URL = "https://maps.googleapis.com/maps/api/staticmap" +
                    "?sensor=true" +
                    "&scale=1" +
                    "&style=element:geometry%%7Cinvert_lightness:true" +
                    "&style=feature:landscape.natural.terrain%%7Celement:geometry%%7Cvisibility:on" +
                    "&style=feature:landscape%%7Celement:geometry.fill%%7Ccolor:0x303030" +
                    "&style=feature:poi%%7Celement:geometry.fill%%7Ccolor:0x404040" +
                    "&style=feature:poi.park%%7Celement:geometry.fill%%7Ccolor:0x0a330a" +
                    "&style=feature:water%%7Celement:geometry%%7Ccolor:0x00003a" +
                    "&style=feature:transit%%7Celement:geometry%%7Cvisibility:on%%7Ccolor:0x101010" +
                    "&style=feature:road%%7Celement:geometry.stroke%%7Cvisibility:on" +
                    "&style=feature:road.local%%7Celement:geometry.fill%%7Ccolor:0x606060" +
                    "&style=feature:road.arterial%%7Celement:geometry.fill%%7Ccolor:0x888888" +
                    "&size=640x360" +
                    "&center=%.5f,%.5f" +
                    "&zoom=%d" +
                    "&markers=color:green%%7C" +
                    "label:G%%7C%.5f,%.5f";
            HIGH_CONTRAST.URL = "http://maps.googleapis.com/maps/api/staticmap" +
                    "?center=%.5f,%.5f" +
                    "&zoom=%d" +
                    "&scale=1" +
                    "&size=630x360" +
                    "&maptype=roadmap" +
                    "&markers=color:green%%7Clabel:G%%7C%.5f,%.5f" +
                    "&sensor=false" +
                    "&style=feature:road%%7Celement:geometry%%7Ccolor:0x005500%%7Cweight:1%%7Cvisibility:on" +
                    "&style=feature:road.arterial%%7Celement:geometry%%7Ccolor:0x005500%%7Cweight:2%%7Cvisibility:on" +
                    "&style=feature:road.highway%%7Celement:geometry%%7Ccolor:0x005500%%7Cweight:3%%7Cvisibility:on" +
                    "&style=feature:landscape%%7Celement:geometry.fill%%7Ccolor:0x000000%%7Cvisibility:on" +
                    "&style=feature:administrative%%7Celement:labels%%7Cweight:3.9%%7Cvisibility:on%%7Cinvert_lightness:true" +
                    "&style=feature:poi%%7Cvisibility:simplified";
            SATELLITE.URL = "http://maps.googleapis.com/maps/api/staticmap" +
                    "?center=%.5f,%.5f" +
                    "&zoom=%d" +
                    "&scale=1" +
                    "&size=630x360" +
                    "&maptype=satellite" +
                    "&markers=color:green%%7Clabel:G%%7C%.5f,%.5f" +
                    "&sensor=true";
            TERRAIN.URL = "http://maps.googleapis.com/maps/api/staticmap" +
                    "?center=%.5f,%.5f" +
                    "&zoom=%d" +
                    "&scale=1" +
                    "&size=630x360" +
                    "&maptype=terrain" +
                    "&markers=color:green%%7Clabel:G%%7C%.5f,%.5f" +
                    "&sensor=true";
        }
    }

    private static final int CACHE_SIZE = 100 * 1024; // 100MiB
    private LruCache<String, Bitmap> mapCache;
    private MapType mMapType;

    private static MapImageManager sSelf;

    public interface ImageFoundCallback {
        void callback(Bitmap mapBitmap);
    }

    public class ImageNotCachedException extends Exception { }


    private MapImageManager() {
        mapCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
        mMapType = MapType.HIGH_CONTRAST;
    }

    /**
     * Gets an instance of the MapImageManager singleton, creating it if necessary.
     */
    public static synchronized MapImageManager getInstance() {
        if (sSelf == null) {
            sSelf = new MapImageManager();
        }
        return sSelf;
    }

    /**
     * Gets a map asynchronously. Once the map image is available, passes it to the supplied
     * callback.
     */
    public void getMap(Location location, int zoom, ImageFoundCallback callback) {
        Bitmap cachedMap = mapCache.get(toCacheKey(location, zoom));
        if (cachedMap == null) {
            fetchMap(location, zoom, callback);
        } else {
            callback.callback(cachedMap);
        }
    }

    /**
     * Sets the type of map fetched. This also purges the cache.
     */
    public void setMapType(MapType mapType) {
        mMapType = mapType;
        // TODO: instead of purging the cache, why not add the type to to the key?
        mapCache.evictAll();
    }

    /**
     * Gets a map image and return it right away, but only if it's already cached.
     * @throws ImageNotCachedException when the map image isn't available in the cache
     */
    public Bitmap getMapIfCached(Location location, int zoom) throws ImageNotCachedException {
        Bitmap cachedMap = mapCache.get(toCacheKey(location, zoom));
        if (cachedMap != null) {
            return cachedMap;
        } else {
            throw new ImageNotCachedException();
        }
    }

    /**
     * Primes the cache by fetching images you will need shortly
     */
    public void preFetch(Location location, int zoom) {
        Bitmap cachedMap = mapCache.get(toCacheKey(location, zoom));
        if (cachedMap == null) {
            fetchMap(location, zoom, null);
        }
    }

    private void fetchMap(final Location location, final int zoom, final ImageFoundCallback callback) {
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... urls) {
                try {
                    HttpResponse response = new DefaultHttpClient().execute(new HttpGet(urls[0]));

                    InputStream is = response.getEntity().getContent();

                    return BitmapFactory.decodeStream(is);
                } catch (Exception e) {
                    Log.v(TAG, "Failed to load image: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    mapCache.put(toCacheKey(location, zoom), bitmap);
                    if (callback != null) {
                        callback.callback(bitmap);
                    }
                }
            }
        }.execute(toStaticMapsApiUrl(location, zoom));
    }

    private String toCacheKey(Location location, int zoom) {
        return location.getLatitude() + "," + location.getLongitude() + ":" + zoom;
    }

    private String toStaticMapsApiUrl(Location location, int zoom) {
        return String.format(mMapType.URL, location.getLatitude(), location.getLongitude(),
                zoom, location.getLatitude(), location.getLongitude());
    }
}
