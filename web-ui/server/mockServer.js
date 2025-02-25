const WebSocket = require('ws');

// Create a WebSocket server that listens on port 8082
const wss = new WebSocket.Server({ port: 8082 });

wss.on('connection', (ws) => {
    console.log("New client connected");

    ws.on('message', (message) => {
        console.log("Received message:", message.toString()); 
        // console.log("Received message:", message);
    });

    ws.on('close', () => {
        console.log("Client disconnected");
    });
});

console.log("Mock WebSocket server running on ws://localhost:8082");

