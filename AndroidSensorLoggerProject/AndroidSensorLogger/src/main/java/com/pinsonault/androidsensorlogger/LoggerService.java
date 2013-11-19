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
    public static final long LOG_INTERVAL = 2000;
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
    private File mLogFile;
    private BufferedWriter mLogFileBuffer;


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

        startLogging();
    }

    @Override
    public void onDestroy() {

        try {
            mLogFileBuffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        pauseLogging();

        // Tell the user we stopped.
        Toast.makeText(this, R.string.logger_service_stopped, Toast.LENGTH_SHORT).show();
    }

    /**************************************
     Logging management functions
     **************************************/

    private void startLogging() {
        // Start collecting sensor data, storing it in memory
        openLogFile();
        setupSensors();
        resumeLogging();
    }

    private void resumeLogging() {
        resetLoggingData();
        registerListeners();
        startLoggerTimer();
    }

    private void pauseLogging() {
        // Stop collecting sensor data

        mTimer.cancel();
        unregisterListeners();
    }

    private void startLoggerTimer() {
        if(mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }

        mTimer.scheduleAtFixedRate(new LogSensorDataTimer(), 0, LOG_INTERVAL);
    }

    private void resetLoggingData() {
        mLightReadingCumulative = 0.0f;
        mLightReadingCount = 0;

        mProximityReadingCumulative = 0.0f;
        mProximityReadingCount = 0;

        // Java defaults arrays to 0
        mAccelerometerReadingCumulative = new float[3];
        mAccelerometerReadingCount = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("LoggerService", "onBind");
        return mBinder;
    }

    private void setupSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void setupActivitySensor() {

    }


    /**************************************
     Sensor/Listener Functions
     **************************************/
    SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                mLightReadingCumulative += event.values[0];
                mLightReadingCount++;
            }

            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                mProximityReadingCumulative += event.values[0];
                mProximityReadingCount++;
            }

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                for (int i = 0; i < 3; i++){
                    mAccelerometerReadingCumulative[i] += event.values[i];
                }
                mAccelerometerReadingCount++;
            }
        }
    };

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

    class LogSensorDataTimer extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    pauseLogging();
                    Toast.makeText(getApplicationContext(), makeLogLine(),
                            Toast.LENGTH_SHORT).show();
                    // Write to the log file
                    appendLogFile();
                    resumeLogging();
                }
            });
        }
    }

    /**************************************
        Log File Functions
    **************************************/

    private void openLogFile() {
        mLogFile = new File("sdcard/sensor_log.txt");
        try {
            mLogFileBuffer = new BufferedWriter(new FileWriter(mLogFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!mLogFile.exists())
        {
            try
            {
                mLogFile.createNewFile();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public String getCurrentTime() {
        SimpleDateFormat dateformat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy", Locale.US);
        return (dateformat.format(new Date()));
    }

    private void appendLogFile() {
        try
        {
            // BufferedWriter for performance, true to set append to file flag
            mLogFileBuffer.append(makeLogLine());
            mLogFileBuffer.newLine();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String makeLogLine() {
        float lightSensorAvg = mLightReadingCumulative / mLightReadingCount;
        float proximitySensorAvg = mProximityReadingCumulative / mProximityReadingCount;

        float[] accelerometerSensorAvg = new float[3];
        for (int i = 0; i < 3; i++) {
            accelerometerSensorAvg[i] = mAccelerometerReadingCumulative[i] / mAccelerometerReadingCount;
        }

        String timeStamp = getCurrentTime();

        return String.format("%s,%f,%f,%f,%f,%f", timeStamp, lightSensorAvg, proximitySensorAvg,
                accelerometerSensorAvg[0], accelerometerSensorAvg[1],accelerometerSensorAvg[2]);
    }
}
