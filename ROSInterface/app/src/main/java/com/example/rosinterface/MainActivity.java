package com.example.rosinterface;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ainirobot.coreservice.client.ApiListener;
import com.ainirobot.coreservice.client.Definition;
import com.ainirobot.coreservice.client.RobotApi;
import com.ainirobot.coreservice.client.StatusListener;
//import com.ainirobot.coreservice.client.actionbean.Pose;
import com.example.rosinterface.maputils.GsonUtil;
//import com.example.rosinterface.maputils.Pose;
import com.ainirobot.coreservice.client.listener.CommandListener;
import com.ainirobot.coreservice.client.actionbean.Pose;
import com.ainirobot.coreservice.client.module.ModuleCallbackApi;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);
    private static MainActivity mInstance;;
    private volatile int reqId = 0;
    private static final String TAG = "MainActivity";
    private RobotWebSocketServer webSocketServer;
    private CommandReceiver commandReceiver;
    private Button exitButton;
    private static boolean isInNoGoZone = false;
    private static boolean isLocked = false;
    private static boolean hasToggledLock = false;
    public Pose no_go_point1_1;
    public Pose no_go_point1_2;
    public Pose no_go_point2_1;
    public Pose no_go_point2_2;
    private int websocket_port = 8081;

    private Handler mHandler;
    private Runnable mLocationRunnable;

    public static MainActivity getInstance(){
        return mInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());  // Handler on the main thread
        // Button init
        exitButton = findViewById(R.id.exit_button);
        no_go_point1_1 = new Pose();
        no_go_point1_2 = new Pose();
        no_go_point2_1 = new Pose();
        no_go_point2_2 = new Pose();

        // Start the WebSocket server
        webSocketServer = new RobotWebSocketServer(this,websocket_port);
        webSocketServer.setWebSocketListener(new RobotWebSocketServer.WebSocketListener() {
            @Override
            public void onOpen() {
                Log.d(TAG, "WebSocket connected");
            }
            @Override
            public void onClose() {
                Log.d(TAG, "WebSocket disconnected");
            }
        });
        webSocketServer.start();

        // start websocket if it is not connected
        if (!isWebSocketConnected()) {
            // If WebSocket is not connected, start the service
            Log.d(TAG, "Websocket is not connected, reconnecting...");
        }

        // Connect to Robot API
        RobotApi.getInstance().connectServer(this, new ApiListener() {
            @Override
            public void handleApiDisabled() {
            }
            @Override
            public void handleApiConnected() {
               // Robot API connected
                readNoGoPoints("nogo1_1","nogo1_2",no_go_point1_1,no_go_point1_2);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        readNoGoPoints("nogo2_1", "nogo2_2", no_go_point2_1, no_go_point2_2);
                    }
                }, 1000);

                // Introduce a 3-second delay before starting to register the status listener continuously
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Start registering the status listener continuously
                        registerStatusListener();
                        mHandler.postDelayed(this, 3000);  // 3000ms = 3 seconds delay after reading the location
                    }
                }, 3000);
            }
            @Override
            public void handleApiDisconnected() {
                //Disconnect
            }
        });

        // Exit button behaviour
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop WebSocket service
                try {
                    webSocketServer.stop();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Remove the location request runnable if activity is destroyed
                mHandler.removeCallbacks(mLocationRunnable);
                // Close the app
                finish();
            }
        });
    }

    public static boolean isInNoGoZone() {
        return isInNoGoZone;
    }
    public static boolean isLocked() {
        return isLocked;
    }
    public static void setLocked(boolean lock) {
        isLocked = lock;
    }
    public static void setLockToggle(boolean lock) {
        hasToggledLock = lock;
    }
    public float cal_displacement(float x1, float y1, float x2, float y2, float px, float py) {
        // Calculate the line equation components
        float numerator = Math.abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1);
        float denominator = (float) Math.sqrt(Math.pow((y2 - y1), 2) + Math.pow((x2 - x1), 2));
        // Return the distance (displacement)
        return numerator / denominator;
    }

    public boolean isInBufferZone(Pose robotPose, float buffer) {  // Determine if the robot is in the buffer zone
        float robotX = robotPose.getX();
        float robotY = robotPose.getY();
        // Get the min/max coordinates of the rectangle
        float minX = Math.min(Math.min(no_go_point1_1.getX(), no_go_point1_2.getX()), Math.min(no_go_point2_1.getX(), no_go_point2_2.getX())) - buffer;
        float maxX = Math.max(Math.max(no_go_point1_1.getX(), no_go_point1_2.getX()), Math.max(no_go_point2_1.getX(), no_go_point2_2.getX())) + buffer;
        float minY = Math.min(Math.min(no_go_point1_1.getY(), no_go_point1_2.getY()), Math.min(no_go_point2_1.getY(), no_go_point2_2.getY())) - buffer;
        float maxY = Math.max(Math.max(no_go_point1_1.getY(), no_go_point1_2.getY()), Math.max(no_go_point2_1.getY(), no_go_point2_2.getY())) + buffer;

        // Check if the robot is inside the expanded rectangle (actual zone + buffer)
        return (robotX >= minX && robotX <= maxX && robotY >= minY && robotY <= maxY);
    }

    private void readNoGoPoints(String point1,String point2, Pose pose1,Pose pose2) {
        // Fetch location for nogo1_1
        RobotApi.getInstance().getLocation(0, point1, new CommandListener() {
            @Override
            public void onResult(int result, String message) {
                try {
                    JSONObject point = new JSONObject(message);
                    pose1.setX((float) point.getDouble(Definition.JSON_NAVI_POSITION_X));
                    pose1.setY((float) point.getDouble(Definition.JSON_NAVI_POSITION_Y));
                    pose1.setTheta((float) point.getDouble(Definition.JSON_NAVI_POSITION_THETA));
                    Log.d("points", "nogo1_1: X=" + pose1.getX() + ", Y=" + pose1.getY());

                    // After getting the first point, fetch the second one
                    fetchNoGoPoint2(point2,pose2);
                } catch (Exception e) {
                    Log.e("points", "Error reading "+point1, e);
                }
            }
        });
    }

    private void fetchNoGoPoint2(String point2, Pose pose2) {
        RobotApi.getInstance().getLocation(0, point2, new CommandListener() {
            @Override
            public void onResult(int result, String message) {
                try {
                    JSONObject point = new JSONObject(message);
                    pose2.setX((float) point.getDouble(Definition.JSON_NAVI_POSITION_X));
                    pose2.setY((float) point.getDouble(Definition.JSON_NAVI_POSITION_Y));
                    pose2.setTheta((float) point.getDouble(Definition.JSON_NAVI_POSITION_THETA));
                    Log.d("points", point2+ ": X=" + pose2.getX() + ", Y=" + pose2.getY());
                } catch (Exception e) {
                    Log.e("points", "Error reading"+point2, e);
                }
            }
        });
    }

    private void registerStatusListener() {
        RobotApi.getInstance().registerStatusListener(Definition.STATUS_POSE, new StatusListener() {
            @Override
            public void onStatusUpdate(String type, String value) {
                Pose pose = GsonUtil.fromJson(value, Pose.class);
                boolean inZone = isInBufferZone(pose, 0.01f); // buffer distance
//                Status = 0 - Robot is in normal zone
//                Status = 1 - Robot is outside the map
//                Status = 2 - Robot is in no-go zone
                if (inZone) {
                    Log.e("Displacement", "Robot is within the buffer zone.");
                    isInNoGoZone = true;
                    pose.setStatus(2); // Set No-Go Zone status
                    // Lock the robot if it's not already locked and hasn't been manually unlocked
                    if (!isLocked && !hasToggledLock) {
                        isLocked = true;
                        hasToggledLock = true;  // Mark that the lock action has been performed
                        Log.d("Displacement", "Robot locked in the buffer Zone.");
                    }
                } else { // If robot is outside the No-Go Zone
                    Log.d("Displacement", "Robot is outside the buffer zone.");
                    // Reset toggle flag when robot leaves the No-Go Zone
                    if (isInNoGoZone) {
                        hasToggledLock = false;  // Allow locking again if it re-enters
                        Log.d("Displacement", "Resetting hasToggledLock because robot left the buffer Zone.");
                    }
                    // Unlock only if it wasn’t manually locked
                    isLocked = false;
                    isInNoGoZone = false;
                }
                if (pose.getStatus() == 1) {// Handle robot outside the map
                    Log.e("Displacement", "Robot is outside the map.");
                    // If robot is outside the map, lock it if it wasn't manually unlocked
                    if (!hasToggledLock) {
                        isLocked = true;
                        hasToggledLock = true; // Mark as toggled
                        Log.d("Displacement", "Robot locked outside the map.");
                    }
                    isInNoGoZone = true;
                }
                isInNoGoZone = (pose.getStatus() == 1 || pose.getStatus() == 2);
                Log.d("Displacement", "isLocked: " + isLocked + ", hasToggledLock: " + hasToggledLock);

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
                Log.d(TAG, "WebSocket Server stopped.");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping server", e);
            }
        }
        // Remove any pending callbacks
        if (mHandler != null && mLocationRunnable != null) {
            mHandler.removeCallbacks(mLocationRunnable);
        }
    }

    private boolean isWebSocketConnected() {
        return RobotWebSocketServer.isConnected();
    }

    private CommandListener motionListener = new CommandListener() {
        @Override
        public void onResult(int result, String message) {}
    };

}
