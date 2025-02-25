const statusText = document.getElementById("noGoZoneStatus");
const moveForward = document.getElementById("moveForward");
const turnLeft = document.getElementById("turnLeft");
const stopButton = document.getElementById("stop");
const turnRight = document.getElementById("turnRight");
const moveBackward = document.getElementById("moveBackward");
const unlockButton = document.getElementById("unlockButton");

// All control buttons
const controlButtons = [moveForward, turnLeft, stopButton, turnRight, moveBackward];

// WebSocket connection to the WebSocket proxy (server.js)
const socket = new WebSocket("ws://localhost:8082");

let isLocked = true;  // Lock movement in No-Go Zone by default

socket.onopen = () => {
    console.log("Connected to WebSocket proxy.");
    statusText.textContent = "Connected to WebSocket!";
};

socket.onmessage = (event) => {
    console.log("Message received:", event.data);
    try {
        const data = JSON.parse(event.data);

        // Handle No-Go Zone status
        if (data.noGoZone !== undefined) {
            updateNoGoZoneStatus(data.noGoZone);
        }

        // Check if robot lock status is being sent
        if (data.isLocked !== undefined) {
            isLocked = data.isLocked; // Update isLocked from robot's message
            updateButtonState(); // Update button states based on isLocked
            console.log("Robot lock status updated: isLocked =", isLocked);
        }

        // Automatically unlock when the server sends an unlock command (for any other specific unlock logic)
        if (data.unlock !== undefined && data.unlock === true) {
            isLocked = false; // Unlock the robot
            updateButtonState(); // Enable the buttons
            console.log("Robot unlocked from WebSocket.");
        }
    } catch (error) {
        console.error("Invalid JSON received:", error);
    }
};

socket.onerror = (error) => {
    console.error("WebSocket Error:", error);
};

socket.onclose = () => {
    console.log("WebSocket connection closed.");
    statusText.textContent = "Disconnected from WebSocket.";
};

// Send movement commands to the robot
function sendCommand(command) {
    if (socket.readyState === WebSocket.OPEN) {
        if (!isLocked) { // Only send if controls are unlocked
            socket.send(command);
            console.log("Sent command:", command);
        } else {
            console.log("Movement locked due to No-Go Zone.");
        }
    } else {
        console.log("WebSocket connection is not open.");
    }
}

// Attach event listeners to buttons
moveForward.addEventListener("click", () => sendCommand("up"));
turnLeft.addEventListener("click", () => sendCommand("left"));
stopButton.addEventListener("click", () => sendCommand("stop"));
turnRight.addEventListener("click", () => sendCommand("right"));
moveBackward.addEventListener("click", () => sendCommand("down"));

// Unlock button event (manual unlock)
unlockButton.addEventListener("click", () => {
    isLocked = false; // Unlock robot controls
    sendCommand("unlock"); // Send unlock signal to the robot
    updateButtonState();
    console.log("Robot unlocked manually.");
});

// No-Go Zone handling
function updateNoGoZoneStatus(isInNoGoZone) {
    if (isInNoGoZone) {
        statusText.textContent = "No-Go Zone: Movement disabled!";
        statusText.classList.add("noGoZone");
        isLocked = true; // Lock controls
    } else {
        statusText.textContent = "You are not in the No-Go Zone.";
        statusText.classList.remove("noGoZone");
        isLocked = false; // Unlock controls
    }
    updateButtonState();
}

// Update button state (disable if locked)
function updateButtonState() {
    controlButtons.forEach(button => {
        button.disabled = isLocked;  // Disable control buttons if locked
    });

    // Log the current state of the buttons for debugging
    console.log("Control buttons state updated. isLocked:", isLocked);
}
