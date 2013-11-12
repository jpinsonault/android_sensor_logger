package com.pinsonault.androidsensorlogger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
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
    private boolean mIsBound = false;
    private Sensor mLightSensor;
    private LoggerService mLoggerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
        }

        doBindService();

        setupLightSensor();
    }

    private void setupLightSensor() {
        lightMeter = (ProgressBar) findViewById(R.id.lightmeter);
        textMax = (TextView) findViewById(R.id.max);
        textReading = (TextView) findViewById(R.id.reading);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (mLightSensor == null) {
            Toast.makeText(MainActivity.this,
                    "No Light Sensor! quit-",
                    Toast.LENGTH_LONG).show();
        } else {
            int max = 200;
            lightMeter.setMax(max);
            textMax.setText("Max Reading: " + String.valueOf(max));

            mSensorManager.registerListener(lightSensorEventListener, mLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    SensorEventListener lightSensorEventListener = new SensorEventListener() {

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
                    //textMax.setText("Max Reading: " + String.valueOf(mMax));
                    lightMeter.setMax(mMax);
                }
                lightMeter.setProgress((int) currentReading);
                textReading.setText("Current Reading: " + String.valueOf(currentReading));

                if (mLoggerService != null){
                    textMax.setText("Max Reading: " + mLoggerService.getCurrentTime());
                }
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
        super.onStop();
    }

    private ServiceConnection mLoggerServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mLoggerService = ((LoggerService.LocalBinder)service).getService();

            mIsBound = true;

            // Tell the user about this for our demo.
            //Toast.makeText(this, R.string.logger_service_connected,
            //        Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mLoggerService = null;
            mIsBound = false;
            //Toast.makeText(this, R.string.logger_service_disconnected,
            //        Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Intent loggerServiceIntent = new Intent(this, LoggerService.class);
        startService(loggerServiceIntent);
        bindService(loggerServiceIntent, mLoggerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mLoggerServiceConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

}
