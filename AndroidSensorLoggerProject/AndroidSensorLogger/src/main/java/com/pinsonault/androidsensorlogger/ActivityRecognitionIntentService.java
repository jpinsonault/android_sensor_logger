package com.pinsonault.androidsensorlogger;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
* Service that receives ActivityRecognition updates. It receives updates
* in the background, even if the main Activity is not visible.
*/
public class ActivityRecognitionIntentService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ActivityRecognitionIntentService(String name) {
        super(name);
    }
    //..
    /**
     * Called when a new activity detection update is available.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //...
        // If the intent contains an update
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Get the updatenex
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            // Get the confidence % (probability)
            int confidence = mostProbableActivity.getConfidence();

            // Get the type
            int activityType = mostProbableActivity.getType();

            Intent sendToLoggerIntent = new Intent("com.pinsonault.androidsensorlogger.ACTIVITY_RECOGNITION_DATA");
            sendToLoggerIntent.putExtra("Activity", getFriendlyName((activityType)));
            sendToLoggerIntent.putExtra("Confidence", confidence);
            sendBroadcast(sendToLoggerIntent);
        }
    }

    private static String getFriendlyName(int detected_activity_type) {
        switch (detected_activity_type) {
            case DetectedActivity.IN_VEHICLE:
                return "vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "bike";
            case DetectedActivity.ON_FOOT:
                return "foot";
            case DetectedActivity.TILTING:
                return "tilting";
            case DetectedActivity.STILL:
                return "still";
            default:
                return "unknown";
        }
    }
}
