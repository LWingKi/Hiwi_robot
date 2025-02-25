package com.example.rosinterface;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.Objects;

import com.ainirobot.coreservice.client.ApiListener;
import com.ainirobot.coreservice.client.Definition;
import com.ainirobot.coreservice.client.RobotApi;
import com.ainirobot.coreservice.client.listener.CommandListener;

public class RobotWebSocketServer extends WebSocketServer {
    private int reqId = 0;
    private static final String TAG = "RobotWebSocketServer";
    private Context context;
    private static boolean isConnected = false;
    private WebSocketListener listener;
    private Handler positionHandler = new Handler();
    private Runnable positionUpdater;
    private static final int POSITION_UPDATE_INTERVAL = 500;
    private WebSocket client = null; // Store the client

    public RobotWebSocketServer(Context context, int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        this.context = context;


        // Runnable to send position updates at fixed intervals
        positionUpdater = new Runnable() {
            @Override
            public void run() {
                if (client != null && client.isOpen()) {
                    sendNoGoZoneFlag();  // Send No-Go Zone flag
                    sendLockState(); // Send the Lock flag
                    positionHandler.postDelayed(this, POSITION_UPDATE_INTERVAL);
                } else {
                    Log.w(TAG, "No client connected, stopping position updates.");
                }
            }
        };

    }
    // Setter for WebSocket listener
    public void setWebSocketListener(WebSocketListener listener) {
        this.listener = listener;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, "New connection opened: " + conn.getRemoteSocketAddress());
        client = conn;
        Log.d(TAG, "Assigned ROS WebSocket client.");
        if (listener != null) {
            listener.onOpen();
        }
        // Start sending position updates only when ROS connects
        positionHandler.post(positionUpdater);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "Connection closed: " + conn.getRemoteSocketAddress());
        if (conn == client) {
            client = null;
            Log.d(TAG, "ROS WebSocket client disconnected.");

            if (listener != null) {
                listener.onClose();
            }

            // Stop position updates when ROS disconnects
            positionHandler.removeCallbacks(positionUpdater);
        }
    }

    @Override
    // Receive command from thw webscoket
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, "Received message: " + message);
        if (MainActivity.isInNoGoZone()) {  // Only stop if in no-go zone and the robot is unlocked
            if(MainActivity.isLocked()){
                RobotApi.getInstance().stopMove(0, motionListener);
                if(Objects.equals(message, "unlock")){
                    sendCommandToRobot(message);
                    Log.d(TAG,"Unlock command received. Sending unlock message to the robot");
                }
            }
            else{ //Robot is unlock to leave the no-go zone
                sendCommandToRobot(message);
            }
        }
        else {
                sendCommandToRobot(message); //Send control command to the robot normally
        }
    }
    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "An error occurred: ", ex);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "WebSocket server started on port " + getPort());
    }

    public static boolean isConnected() {
        return isConnected;
    }


    private void sendCommandToRobot(String action) {
        // This is where you send command to the robot via robot api
        Log.d(TAG, "Command Received: " + action);
        // Make sure robot api is connected
        RobotApi.getInstance().connectServer(context, new ApiListener() {
            @Override
            public void handleApiDisabled() {}
            @Override
            public void handleApiConnected() {
                RobotApi.getInstance().setCallback(new ModuleCallback());
            }
            @Override
            public void handleApiDisconnected() {}
        });
        RobotApi.getInstance().connectApi();
        switch (action) {
            case "up":
                RobotApi.getInstance().goForward(0, 0.15f, 0.2f, motionListener);
                Log.d(TAG, "Move FORWARD");
                break;
            case "down":
                RobotApi.getInstance().goBackward(0, 0.15f, 0.2f, motionListener);
                Log.d(TAG, "Move BACKWARD");
                break;
            case "left":
                RobotApi.getInstance().turnLeft(0, 20f, 20f, motionListener);
                Log.d(TAG, "TURN LEFT");
                break;
            case "right":
                RobotApi.getInstance().turnRight(0, 20f, 20f, motionListener);
                Log.d(TAG, "TURN RIGHT");
                break;
            case "stop":
                RobotApi.getInstance().stopMove(0, motionListener);
                Log.d(TAG, "Robot STOP");
                break;
            case "unlock":
                if (MainActivity.isInNoGoZone()) {
                    // Allow unlocking only if the robot is in the No-Go Zone
                    MainActivity.setLockToggle(true);
                    MainActivity.setLocked(false);
                    Log.d(TAG, "Robot is unlocked and can move out of the No-Go Zone");
                } else {
                    Log.d(TAG, "Robot is already outside the No-Go Zone, no need to unlock.");
                }
                break;


            default:
                Log.d(TAG, "Unknown action: " + action);
                break;
        }
    }

    private CommandListener motionListener = new CommandListener() {
        @Override
        public void onResult(int result, String message) {}
    };

    private void sendNoGoZoneFlag() {
        if (client != null && client.isOpen()) {
            JSONObject flagData = new JSONObject();
            try {
                flagData.put("noGoZone", MainActivity.isInNoGoZone());
                client.send(flagData.toString());
            //Log.d(TAG, "Sent No-Go Zone flag: " + flagData.toString());
            } catch (Exception e) {
                Log.e(TAG, "Error sending No-Go Zone flag", e);
            }
        } else {
            Log.w(TAG, "ROS client not connected, cannot send No-Go Zone flag");
        }
    }
    private void sendLockState() {
        if (client != null && client.isOpen()) {
            JSONObject lockStateData = new JSONObject();
            try {
                lockStateData.put("isLocked", MainActivity.isLocked()); // Send the lock state
                client.send(lockStateData.toString());
                //Log.d(TAG, "Sent lock state: " + isLocked);
            } catch (Exception e) {
                Log.e(TAG, "Error sending lock state", e);
            }
        } else {
            Log.w(TAG, "ROS client not connected, cannot send lock state");
        }
    }

    // Interface for WebSocket connection status events
    public interface WebSocketListener {
        void onOpen();
        void onClose();
    }
}
