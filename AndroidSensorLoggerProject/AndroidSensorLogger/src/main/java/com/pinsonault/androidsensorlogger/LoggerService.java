package com.pinsonault.androidsensorlogger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by joe on 11/5/13.
 */
public class LoggerService extends Service {
    public static final long NOTIFY_INTERVAL = 10 * 1000; // 10 seconds
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    // run on another Thread to avoid crash
    private Handler mHandler = new Handler();
    // timer handling
    private Timer mTimer = null;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mProximitySensor;
    private int mLightReadingCount;
    private float mLightReadingCumulative;
    private int mProximityReadingCount;
    private float mProximityReadingCumulative;
    private int mAccelerometerReadingCount;
    private float[] mAccelerometerReadingCumulative;


    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        LoggerService getService() {
            Log.i("LoggerService", "getService");
            return LoggerService.this;
        }
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, R.string.logger_service_started, Toast.LENGTH_SHORT).show();

        //mTimerHandler = new Handler();
        // cancel if already existed
        if(mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }
        // schedule task
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);

        startLogging();
    }

    private void startLogging() {
        // Start collecting sensor data, storing it in memory
        // A timer run periodically saving the data to disk
        setupSensors();
        resumeLogging();
    }

    private void setupSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void setupActivitySensor() {

    }

    private void setupAccelerometerSensor() {
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometerReadingCount = 0;
        mAccelerometerReadingCumulative = new float[3];

        if (mLightSensor == null) {
            Toast.makeText(getApplicationContext(),
                    "No Accelerometer Sensor!",
                    Toast.LENGTH_LONG).show();
        } else {

            mSensorManager.registerListener(mSensorEventListener, mAccelerometerSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void setupProximitySensor() {
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mProximityReadingCount = 0;
        mProximityReadingCumulative = 0.0f;


        if (mLightSensor == null) {
            Toast.makeText(getApplicationContext(),
                    "No Proximity Sensor!",
                    Toast.LENGTH_LONG).show();
        } else {

            mSensorManager.registerListener(mSensorEventListener, mProximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void setupLightSensor() {
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mLightReadingCount = 0;
        mLightReadingCumulative = 0.0f;

        if (mLightSensor == null) {
            Toast.makeText(getApplicationContext(),
                    "No Light Sensor!",
                    Toast.LENGTH_LONG).show();
        } else {

            mSensorManager.registerListener(mSensorEventListener, mLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void registerListeners() {
        mSensorManager.registerListener(mSensorEventListener, mLightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(mSensorEventListener, mProximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(mSensorEventListener, mAccelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterListeners() {
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    private void resumeLogging() {
        resetLoggingData();

        registerListeners();
    }

    private void pauseLogging() {
        // Stop collecting sensor data
        unregisterListeners();
    }

    private void resetLoggingData() {
        mLightReadingCumulative = 0.0f;
        mLightReadingCount = 0;

        mProximityReadingCumulative = 0.0f;
        mProximityReadingCount = 0;

        mAccelerometerReadingCount = 0;
        mAccelerometerReadingCumulative = new float[3];
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(mSensorEventListener);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.logger_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("LoggerService", "onBind");
        return mBinder;
    }

    public String getCurrentTime() {
        SimpleDateFormat dateformat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy", Locale.US);
        return (dateformat.format(new Date()));
    }

    class TimeDisplayTimerTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // display toast
                    Toast.makeText(getApplicationContext(), getDateTime(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private String getDateTime() {
            // get date time in custom format
            SimpleDateFormat sdf = new SimpleDateFormat("[yyyy/MM/dd - HH:mm:ss]");
            return sdf.format(new Date());
        }
    }

    SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                mLightReadingCumulative = event.values[0];
                mLightReadingCount++;
            }

            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                mProximityReadingCumulative = event.values[0];
                mProximityReadingCount++;
            }

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                for (int i = 0; i < 3; i++){
                    mAccelerometerReadingCumulative[i] = event.values[i];
                }
                mAccelerometerReadingCount++;
            }
        }
    };

    private void openLogFile() {
        // TODO

    }

    private void appendLogFile() {
        File logFile = new File("sdcard/log.file");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            // BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append("");
            buf.newLine();
            buf.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
