# File description
The repo consist of: 
1. ROSInterface : The android app of the robot
2. web-ui : the web interface and the node js server

# How to run the repo

## Running android app
1. Connect the robot with ADB
`adb connect {robot ip}`.

    If this is the first time connecting with the robot:
    `adb connect {robot ip}:5555`
    Then allow your device's access on the robot and choose "Always allow"
2. On the android studio, run the robot application

## Running nodejs and web interface
1. In the terminal, change the directory to `/Hiwi_robot/web-ui/server`. Run `node server.js` to start the nodejs server. You will see the following response:
    ```
    WebSocket proxy server running on ws://localhost:8082
    HTTP server running at: http://localhost:8080
    ```
    This means the web UI is connected to the the nodes server with port `8082`

2. Access the web UI with ``http://localhost:8080`` and you shall see the web UI


TBC