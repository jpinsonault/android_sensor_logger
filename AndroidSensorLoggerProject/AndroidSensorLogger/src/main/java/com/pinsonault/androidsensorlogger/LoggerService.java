package com.pinsonault.androidsensorlogger;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;

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
public class LoggerService extends Service implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener{
    public static final long LOG_INTERVAL = 5000;
    private static final String TAG = "LoggerService";
    private Context mContext;

    private BroadcastReceiver mActivityRecognitionReciever;

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
    private FileWriter mLogFileBuffer;
    private String mLogFileName = "sensor_log.txt";

    private static ActivityRecognitionClient mActivityRecognitionClient;
    private static PendingIntent mCallbackIntent;

    private static int ACTIVITY_UPDATE_INTERVAL = 30*1000;
    private int mLatestActivity;
    private int mLatestActivityConfidence;


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
        mContext = getApplicationContext();
        setupActivitySensor();
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
        stopActivityRecognitionScan();
        unregisterReceiver(mActivityRecognitionReciever);

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
        }

        // recreate new
        mTimer = new Timer();


        mTimer.scheduleAtFixedRate(new LogSensorDataTimer(), LOG_INTERVAL, LOG_INTERVAL);
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
        mLatestActivity = 0;
        mLatestActivityConfidence = 0;

        mActivityRecognitionReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLatestActivity = intent.getExtras().getInt("Activity");
                mLatestActivityConfidence = intent.getExtras().getInt("Confidence");

                Log.i("################# Got a response", Integer.toString(mLatestActivity));
            }
        };

        IntentFilter filter = new IntentFilter();
        Log.i("setupActivitySensor", "Before registerReciever");
        filter.addAction("com.pinsonault.androidsensorlogger.ACTIVITY_RECOGNITION_DATA");
        registerReceiver(mActivityRecognitionReciever, filter);

        startActivityRecognitionScan();
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
                    //Toast.makeText(getApplicationContext(), makeLogLine(),
                    //        Toast.LENGTH_SHORT).show();
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
        mLogFile = new File(getApplicationContext().getFilesDir(), mLogFileName);

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

        try {
            mLogFileBuffer = new FileWriter(mLogFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentTime() {
        SimpleDateFormat dateformat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy", Locale.US);
        return (dateformat.format(new Date()));
    }

    private void appendLogFile() {
        try
        {
            openLogFile();
            // BufferedWriter for performance, true to set append to file flag
            mLogFileBuffer.write(makeLogLine());

            mLogFileBuffer.close();
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

        return String.format("%s,%f,%f,%f,%f,%f,%d,%d\n", timeStamp, lightSensorAvg, proximitySensorAvg,
                accelerometerSensorAvg[0], accelerometerSensorAvg[1],accelerometerSensorAvg[2],
                mLatestActivity, mLatestActivityConfidence);
    }

    /**
     * Call this to start a scan - don't forget to stop the scan once it's done.
     * Note the scan will not start immediately, because it needs to establish a connection with Google's servers - you'll be notified of this at onConnected
     */
    public void startActivityRecognitionScan(){

        mActivityRecognitionClient	= new ActivityRecognitionClient(mContext, this, this);
        mActivityRecognitionClient.connect();
        Log.i(TAG, "startActivityRecognitionScan");
    }

    public void stopActivityRecognitionScan(){
        try{
            mActivityRecognitionClient.removeActivityUpdates(mCallbackIntent);
            Log.i(TAG,"stopActivityRecognitionScan");
        } catch (IllegalStateException e){
            // probably the scan was not set up, we'll ignore
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG,"onConnectionFailed");
    }

    /**
     * Connection established - start listening now
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Intent intent = new Intent(mContext, ActivityRecognitionIntentService.class);
        mCallbackIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mActivityRecognitionClient.requestActivityUpdates(ACTIVITY_UPDATE_INTERVAL, mCallbackIntent);
    }

    @Override
    public void onDisconnected() {
    }
}
