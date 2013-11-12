package com.pinsonault.androidsensorlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by joe on 11/8/13.
 */
public class LoggerServiceReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, LoggerService.class);
        context.startService(startServiceIntent);
    }
}
