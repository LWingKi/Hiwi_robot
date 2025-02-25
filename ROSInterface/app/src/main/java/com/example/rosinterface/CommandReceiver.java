package com.example.rosinterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ainirobot.coreservice.client.RobotApi;
import com.ainirobot.coreservice.client.listener.CommandListener;// ADB commmand handling for testing only

public class CommandReceiver extends BroadcastReceiver {
    private static final String TAG = "CommandReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.example.rosinterface.COMMAND".equals(action)) {
            String command = intent.getStringExtra("command");
            Log.d(TAG, "Received command: " + command);
        }
    }
}