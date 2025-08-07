# Java Chat Application

A simple multi-user chat application built with Java socket programming that allows multiple clients to connect to a central server and exchange messages in real-time.

![java chat app](https://github.com/user-attachments/assets/a3294f08-b19e-488d-8583-fbc34e7579f8)

## Project Overview

This project implements a client-server chat application with the following components:

- **ChatServer**: Manages multiple client connections and handles message broadcasting
- **ChatClient**: Provides a text-based interface for users to send and receive messages

## Architecture

### Server Implementation (ChatServer.java)

- Uses `ServerSocket` to accept incoming client connections
- Assigns unique user IDs to each connected client
- Maintains a thread-safe list of connected users using `ConcurrentHashMap`
- Implements message broadcasting to all connected clients
- Handles client disconnections gracefully
- Uses thread pool (`ExecutorService`) for concurrent client handling

### Client Implementation (ChatClient.java)

- Connects to server using `Socket` programming
- Provides simple text-based user interface
- Handles username registration and validation
- Sends messages to server for broadcasting
- Receives messages from other users in real-time
- Supports graceful disconnection with 'quit' command

## How to Run the Application

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Command line terminal or IDE

### Step 1: Compile the Java Files

```bash
javac ChatServer.java
javac ChatClient.java
```

### Step 2: Start the Server

Open a terminal window and run:

```shellscript
java ChatServer
```

You should see output similar to:

```plaintext
=== Chat Server Started ===
Server listening on port: 12345
Maximum clients allowed: 50
Waiting for client connections...
```

### Step 3: Start Client(s)

Open additional terminal widows (one for each client) and run:

```shellscript
java ChatClient
```

You need **at least 3 terminal windows**:

- 1 for the server
- 2+ for different clients

### Step 4: Using the Chat Application

1. When a client starts, you'll see a welcome message
2. Enter a unique username when prompted
3. Start typing messages to chat with other connected users
4. Type 'quit' to exit the chat application

## User Interface Screenshots

### Server Console Output

```plaintext
=== Chat Server Started ===
Server listening on port: 12345
Maximum clients allowed: 50
Waiting for client connections...

New client connected - User ID: 1 | Total clients: 1
New client connected - User ID: 2 | Total clients: 2
Message broadcasted from User 1 (Alice): Hello everyone!
Message broadcasted from User 2 (Bob): Hi Alice!
```

### Client Console Interface

```plaintext
╔══════════════════════════════════════╗
║          JAVA CHAT CLIENT            ║
║                                      ║
║  Instructions:                       ║
║  1. Enter a unique username          ║
║  2. Start chatting with others       ║
║  3. Type 'quit' to exit              ║
╚══════════════════════════════════════╝

=== Chat Client Starting ===
Attempting to connect to server at localhost:12345
Successfully connected to chat server!

=== Welcome to the Chat Server ===
Your User ID is: 1
Please enter your username:
> Alice
Username accepted! You are now connected to the chat.
Type your messages below (type 'quit' to exit):

=== Chat Session Started ===
You can now start chatting! Type 'quit' to exit.

> Hello everyone!
[Bob]: Hi Alice! How are you?
> I'm doing great, thanks for asking!
[Charlie]: Hey everyone!
> Hi Charlie! Welcome to the chat!
```

## Technical Implementation Details

### Socket Programming Concepts Used

- **ServerSocket**: Listens for incoming client connections on port 12345
- **Socket**: Establishes communication channel between client and server
- **BufferedReader**: Reads text messages from input streams
- **PrintWriter**: Sends text messages through output streams

### Concurrency and Thread Management

- **ExecutorService**: Thread pool manages multiple client connections efficiently
- **ClientHandler**: Each client runs in its own thread for concurrent message processing
- **Thread-safe Collections**: `ConcurrentHashMap` and `ConcurrentHashMap.newKeySet()` ensure safe access to shared data

### User Management Features

- **Unique User IDs**: Each client receives a sequential unique identifier
- **Username Validation**: Prevents duplicate usernames and empty names
- **Connection Tracking**: Server maintains real-time list of connected users
- **Graceful Disconnection**: Proper cleanup when clients leave the chat

### Message Broadcasting System

- **Real-time Messaging**: Messages are instantly broadcast to all connected clients
- **Message Formatting**: Includes sender username for message identification
- **System Notifications**: Announces when users join or leave the chat
- **Input Validation**: Handles empty messages and special commands

## File Structure

```plaintext
java-chat-application/
├── ChatServer.java         # Server implementation
├── ChatClient.java         # Client implementation
└── README.md               # This documentation file
```

## Core Features

**Server Implementation**

- ChatServer class with socket programming
- Multiple client connection handling
- Unique user ID assignment
- Connected user list maintenance

**Client Implementation**

- ChatClient class with server connection
- Message sending capability
- Message receiving from other users
- Real-time message broadcasting

**User Interface**

- Simple text-based interface
- Message input and display functionality
- Clear user instructions and feedback

**Additional Features**

- **Username Management**: Unique username validation and registration
- **Connection Limits**: Maximum client limit to prevent server overload
- **Graceful Shutdown**: Proper resource cleanup on exit
- **Error Handling**: Comprehensive exception handling throughout
- **Thread Safety**: Safe concurrent access to shared resources

## Troubleshooting

**"Connection refused" error:**

- Ensure the server is running before starting clients
- Check that port 12345 is not blocked by firewall

**"Address already in use" error:**

- Wait a few seconds after stopping the server before restarting
- Make sure no other application is using port 12345

**Messages not appearing:**

- Verify multiple clients are connected
- Check that usernames were accepted successfully
