package com.pinsonault.androidsensorlogger;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;

public class ActivityRecognitionScan implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener{
    private Context context;
    private static final String TAG = "ActivityRecognition";
    private static ActivityRecognitionClient mActivityRecognitionClient;
    private static PendingIntent mCallbackIntent;

    private static int UPDATE_INTERVAL = 60*1000;

    public ActivityRecognitionScan(Context context) {
        this.context=context;
    }

    /**
     * Call this to start a scan - don't forget to stop the scan once it's done.
     * Note the scan will not start immediately, because it needs to establish a connection with Google's servers - you'll be notified of this at onConnected
     */
    public void startActivityRecognitionScan(){
        mActivityRecognitionClient	= new ActivityRecognitionClient(context, this, this);
        mActivityRecognitionClient.connect();
        Log.d(TAG, "startActivityRecognitionScan");
    }

    public void stopActivityRecognitionScan(){
        try{
            mActivityRecognitionClient.removeActivityUpdates(mCallbackIntent);
            Log.d(TAG,"stopActivityRecognitionScan");
        } catch (IllegalStateException e){
            // probably the scan was not set up, we'll ignore
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG,"onConnectionFailed");
    }

    /**
     * Connection established - start listening now
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Intent intent = new Intent(context, ActivityRecognitionIntentService.class);
        mCallbackIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mActivityRecognitionClient.requestActivityUpdates(UPDATE_INTERVAL, mCallbackIntent);
    }

    @Override
    public void onDisconnected() {
    }

}