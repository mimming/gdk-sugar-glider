package com.mimming.sugarglider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by mimming on 9/22/13.
 */
public class StopActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stopService(new Intent(this, MapDisplayService.class));
        this.finish();
    }
}
