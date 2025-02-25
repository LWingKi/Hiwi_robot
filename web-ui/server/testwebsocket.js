const WebSocket = require('ws');

const ROBOT_WS_URL = 'ws://10.151.0.173:8081';

const robotSocket = new WebSocket(ROBOT_WS_URL);

robotSocket.on('open', () => {
    console.log('Connected to robot WebSocket!');
});

robotSocket.on('message', (message) => {
    console.log('Received message from robot:', message);
});

robotSocket.on('error', (error) => {
    console.error('Error connecting to robot WebSocket:', error);
});

robotSocket.on('close', () => {
    console.log('Disconnected from robot WebSocket');
});
