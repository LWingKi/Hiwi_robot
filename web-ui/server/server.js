const http = require('http');
const path = require('path');
const fs = require('fs');
const WebSocket = require('ws');

// Ports configuration
const HTTP_PORT = 8080;  // Web UI (Browser). DO NOT CHANGE  
const WS_PORT = 8082;    // WebSocket Proxy. DO NOT CHANGE  
const ROBOT_WS_URL = "10.151.0.173" // Robot WebSocket. Chaneg to your robot's IP address

// Store No-Go Zone status
let isInNoGoZone = false;
let robotSocket = null;

// HTTP Server to serve HTML, JS, and CSS files
const server = http.createServer((req, res) => {
    let filePath = path.join(__dirname, '..', 'public', req.url === '/' ? 'index.html' : req.url);

    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(404);
            res.end('File not found');
        } else {
            res.writeHead(200, { 'Content-Type': getContentType(filePath) });
            res.end(data);
        }
    });
});

server.listen(HTTP_PORT, () => {
    console.log(`HTTP server running at: http://localhost:${HTTP_PORT}`);
});

// WebSocket Proxy Server (Forward messages between Web UI & Robot)
const wss = new WebSocket.Server({ port: WS_PORT });

wss.on('connection', (clientSocket) => {
    console.log('Web UI connected to WebSocket proxy');

    // Ensure the robot WebSocket is connected
    connectToRobot();

    // Send initial No-Go Zone status
    clientSocket.send(JSON.stringify({ noGoZone: isInNoGoZone }));

    clientSocket.on('message', (message) => {
        console.log(`Received message from Web UI: ${message}`);
        if (robotSocket && robotSocket.readyState === WebSocket.OPEN) {
            robotSocket.send(message.toString());
        } else {
            console.log('Robot WebSocket is not connected, cannot forward message.');
        }
    });

    clientSocket.on('close', () => {
        console.log('Web UI disconnected');
    });

    clientSocket.on('error', (error) => {
        console.error('Web UI Error:', error);
    });
});

console.log(`WebSocket proxy server running on ws://localhost:${WS_PORT}`);

// Function to connect to the robot WebSocket
function connectToRobot() {
    if (robotSocket && (robotSocket.readyState === WebSocket.OPEN || robotSocket.readyState === WebSocket.CONNECTING)) {
        return; // Already connected or connecting
    }

    // robotSocket = new WebSocket(ROBOT_WS_URL);
    robotSocket = new WebSocket(`ws://${ROBOT_WS_URL}:8081`);

    robotSocket.on('open', () => {
        console.log(`Connected to robot WebSocket at ${ROBOT_WS_URL}:8081`);
    });

    robotSocket.on('message', (message) => {
        console.log(`Raw message from robot:`, message.toString());
    
        let data;
        try {
            data = JSON.parse(message);
        } catch (error) {
            console.error("Invalid JSON from robot:", message.toString());
            return;  // Skip sending invalid messages
        }
    
        // If valid, process No-Go Zone and forward to all Web UI clients
        if (data.noGoZone !== undefined) {
            isInNoGoZone = data.noGoZone;
            console.log(`Updated No-Go Zone status: ${isInNoGoZone}`);
    
            wss.clients.forEach((client) => {
                if (client.readyState === WebSocket.OPEN) {
                    client.send(JSON.stringify({ noGoZone: isInNoGoZone }));
                }
            });
        }
    
        // Forward the valid JSON message to all Web UI clients
        wss.clients.forEach((client) => {
            if (client.readyState === WebSocket.OPEN) {
                client.send(JSON.stringify(data));
            }
        });
    });
    

    robotSocket.on('close', () => {
        console.log('Robot WebSocket disconnected. Attempting to reconnect...');
        setTimeout(connectToRobot, 3000); // Reconnect after 3 seconds
    });

    robotSocket.on('error', (error) => {
        console.error('Robot WebSocket Error:', error);
    });
}

// Function to get proper content type for files
function getContentType(filePath) {
    const ext = path.extname(filePath);
    switch (ext) {
        case '.html': return 'text/html';
        case '.js': return 'application/javascript';
        case '.css': return 'text/css';
        default: return 'text/plain';
    }
}
