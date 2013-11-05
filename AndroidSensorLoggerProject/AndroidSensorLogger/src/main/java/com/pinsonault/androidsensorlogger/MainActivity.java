package com.pinsonault.androidsensorlogger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

    ProgressBar lightMeter;
    TextView textMax, textReading;
    int mMax;
    SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
        }

        lightMeter = (ProgressBar) findViewById(R.id.lightmeter);
        textMax = (TextView) findViewById(R.id.max);
        textReading = (TextView) findViewById(R.id.reading);

        SensorManager mSensorManager
                = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor
                = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor == null) {
            Toast.makeText(MainActivity.this,
                    "No Light Sensor! quit-",
                    Toast.LENGTH_LONG).show();
        } else {
            int max = 200;
            lightMeter.setMax(max);
            textMax.setText("Max Reading: " + String.valueOf(max));

            mSensorManager.registerListener(lightSensorEventListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    SensorEventListener lightSensorEventListener
            = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                float currentReading = event.values[0];
                if ((int)currentReading > mMax){
                    mMax = (int)currentReading;
                    textMax.setText("Max Reading: " + String.valueOf(mMax));
                    lightMeter.setMax(mMax);
                }
                lightMeter.setProgress((int) currentReading);
                textReading.setText("Current Reading: " + String.valueOf(currentReading));
            }
        }

    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    @Override
    protected void onStop(){
        mSensorManager.unregisterListener(lightSensorEventListener);
    }

}
