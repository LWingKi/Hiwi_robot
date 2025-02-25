# File description
The repo consist of: 
1. ROSInterface : The android app of the robot
2. web-ui : the web interface and the node js server

### The directory stuture:
```
├──ROSInterfaceapp/src/main/
│  ├──java/com/example/rosinterface
│  │    ├── CommandReceiver.java
│  │    ├── LogTools.java
│  │    ├── MainActivity.java         // robot app's main view
│  │    ├── ModuleCallback.java
│  │    ├── RobotWebSocketServer.java // Websocket communication
│  │    └── ui
│  └──res/layout
│       ├── activity_main.xml         // UI design of the main app view
│       ├── back_button.xml           // button design for dynamix use
│
├── web-ui
    ├── public
    │   ├── assets/
    │   ├── css/                      // css design of the UI
    │   ├── js/                       // main.js is in here to define the action of UI interaction
    │   └── index.html                // main page html
    └── server
        ├── mockServer.js              
        ├── node_modules
        ├── package.json
        ├── package-lock.json
        ├── server.js                 // Actual nodejs server
        └── testwebsocket.js          // Small testing nodejs to test websocket connection


```

# How to run the repo

## 1. Running android app
1. Connect the robot with ADB command: ```adb connect {robot ip}```.

    If this is the first time connecting with the robot:
    `adb connect {robot ip}:5555`
    Then allow your device's access on the robot and choose "Always allow"
2. On the android studio, run the robot application

## 2. Running nodejs and web interface
1. In the terminal, change the directory to `/Hiwi_robot/web-ui/server`. Run `node server.js` to start the nodejs server. You will see the following response:
    ```shell
    WebSocket proxy server running on ws://localhost:8082
    HTTP server running at: http://localhost:8080
    ```
    This means the web UI is connected to the the nodes server with port `8082`

2. Access the web UI with ``http://localhost:8080`` and you shall see the web UI


## Boot up sequence
1. Run the robot app on android studio
2. run nodejs
3. Control robot on web UI in ``http://localhost:8080``

## Shut down sequence
1. Close nodejs server (ctrl+c)
2. stop the robot app on android studio
You must stop the nodejs server first so the websocket is not being occupied after the server closes.

## Caution
1. When changing maps, robot must go back to the charging dock to reposition itself
2. If the robot ip resets or changes, please run NetCheck app. In the disgonises tab(bottom right corner), tap go to see the robot ip and update ``ROBOT_WS_URL`` in `server.js`. Please do not change any port settings.
3. To check websocket connection without running any nodejs, run ```wscat -c ws://{robotip}:8081``. If there is no connection error, you shall see robot is seding message and you can send command for example "left","right","up","down","stop" to control robot movement. This is only to test out the websocket implemetation by bypassing nodejs.


## Resources
1. [Robot API doc](https://doc.orionstar.com/en/knowledge-base-category/apk-development/)
2. [Robot API repo](https://github.com/OrionStarGIT/RobotSample?tab=readme-ov-file#1-directory-structure)