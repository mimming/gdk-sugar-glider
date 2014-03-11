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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class SetMapZoomActivity extends Activity {
    private MapImageManager mMapImageManager;
    private MapDisplayService.MapDisplayBinder mMapDisplayService;

    // Map zooms
    private static final int MIN_ZOOM = 15;
    private static final int MAX_ZOOM = 20;
    private final SparseArray<View> mMapZooms = new SparseArray<View>();

    private CardScrollView mCardScrollView;
    private AudioManager mAudioManager;
    private GestureDetector mDetector;

    private GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            if (gesture == Gesture.TAP) {
                mAudioManager.playSoundEffect(Sounds.TAP);
                mMapDisplayService.setZoom(mCardScrollView.getSelectedItemPosition() + MIN_ZOOM);
                finish();
                return true;
            }
            return false;
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof MapDisplayService.MapDisplayBinder) {
                mMapDisplayService = (MapDisplayService.MapDisplayBinder) service;

                Location currentLocation = mMapDisplayService.getLastKnownLocation();
                int currentZoom = mMapDisplayService.getZoom();

                // Fetch all of the zoom level maps
                for (int i = 0; i <= MAX_ZOOM - MIN_ZOOM; i++) {
                    // Prime each one with blank image
                    ImageView imageView = new ImageView(SetMapZoomActivity.this);
                    mMapZooms.put(i, imageView);

                    // Get the real map images asynchronously
                    final int finalI = i;
                    mMapImageManager.getMap(currentLocation, finalI + MIN_ZOOM, new MapImageManager.ImageFoundCallback() {
                        @Override
                        public void callback(Bitmap mapBitmap) {
                            ImageView imageView = new ImageView(SetMapZoomActivity.this);
                            imageView.setImageBitmap(mapBitmap);
                            mMapZooms.put(finalI, imageView);

                            // Since we have new data, update the card scroll view
                            mCardScrollView.updateViews(true);
                        }
                    });
                }

                mCardScrollView.activate();
                setContentView(mCardScrollView);

                // Move to the current zoom level
                mCardScrollView.setSelection(currentZoom - MIN_ZOOM);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Do nothing.
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMapImageManager = MapImageManager.getInstance();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mDetector = new GestureDetector(SetMapZoomActivity.this).setBaseListener(mBaseListener);

        mCardScrollView = new CardScrollView(SetMapZoomActivity.this) {
            @Override
            public final boolean dispatchGenericFocusedEvent(MotionEvent event) {
                return mDetector.onMotionEvent(event) ||
                        super.dispatchGenericFocusedEvent(event);
            }
        };

        SetMapZoomScrollAdapter adapter = new SetMapZoomScrollAdapter();
        mCardScrollView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, MapDisplayService.class), mConnection, 0);
    }

    private class SetMapZoomScrollAdapter extends CardScrollAdapter {

        @Override
        public int findIdPosition(Object id) {
            // Not implemented
            return -1;
        }

        @Override
        public int findItemPosition(Object item) {
            return mMapZooms.indexOfValue((View) item);
        }

        @Override
        public int getCount() {
            return mMapZooms.size();
        }

        @Override
        public Object getItem(int position) {
            return mMapZooms.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mMapZooms.get(position);
        }
    }

    @Override
    protected void onStop() {
        super.onDestroy();
        unbindService(mConnection);
    }
}
