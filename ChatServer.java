import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ChatServer class manages multiple client connections and handles message broadcasting
 * This server uses socket programming to create a multi-user chat environment
 * 
 * @author Student Name
 * @version 1.0
 */
public class ChatServer {
    // Server configuration constants
    private static final int SERVER_PORT = 12345;
    private static final int MAX_CLIENTS = 50;
    
    // Server socket for accepting client connections
    private ServerSocket serverSocket;
    
    // Thread-safe collections to manage connected clients
    private final Map<Integer, ClientHandler> connectedClients;
    private final Set<String> activeUsernames;
    
    // Thread pool for handling multiple clients concurrently
    private final ExecutorService clientThreadPool;
    
    // Counter for assigning unique user IDs
    private int nextUserId;
    
    // Server status flag
    private boolean isServerRunning;
    
    /**
     * Constructor initializes server components
     */
    public ChatServer() {
        this.connectedClients = new ConcurrentHashMap<>();
        this.activeUsernames = ConcurrentHashMap.newKeySet();
        this.clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.nextUserId = 1;
        this.isServerRunning = false;
    }
    
    /**
     * Starts the chat server and begins accepting client connections
     */
    public void startServer() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            isServerRunning = true;
            
            System.out.println("=== Chat Server Started ===");
            System.out.println("Server listening on port: " + SERVER_PORT);
            System.out.println("Maximum clients allowed: " + MAX_CLIENTS);
            System.out.println("Waiting for client connections...\n");
            
            // Main server loop - accepts incoming client connections
            while (isServerRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewClientConnection(clientSocket);
                } catch (IOException e) {
                    if (isServerRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server startup failed: " + e.getMessage());
        } finally {
            shutdownServer();
        }
    }
    
    /**
     * Handles a new client connection by creating a unique user ID and client handler
     * @param clientSocket The socket connection from the new client
     */
    private void handleNewClientConnection(Socket clientSocket) {
        if (connectedClients.size() >= MAX_CLIENTS) {
            System.out.println("Maximum client limit reached. Connection rejected.");
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing rejected client socket: " + e.getMessage());
            }
            return;
        }
        
        // Assign unique user ID to the new client
        int assignedUserId = generateUniqueUserId();
        
        // Create and start client handler thread
        ClientHandler newClientHandler = new ClientHandler(clientSocket, assignedUserId, this);
        connectedClients.put(assignedUserId, newClientHandler);
        clientThreadPool.execute(newClientHandler);
        
        System.out.println("New client connected - User ID: " + assignedUserId + 
                          " | Total clients: " + connectedClients.size());
    }
    
    /**
     * Generates a unique user ID for each connecting client
     * @return A unique integer user ID
     */
    private synchronized int generateUniqueUserId() {
        return nextUserId++;
    }
    
    /**
     * Broadcasts a message from one client to all other connected clients
     * @param senderUserId The ID of the client sending the message
     * @param messageContent The message content to broadcast
     * @param senderUsername The username of the message sender
     */
    public void broadcastMessageToAllClients(int senderUserId, String messageContent, String senderUsername) {
        String formattedMessage = "[" + senderUsername + "]: " + messageContent;
        
        // Send message to all connected clients except the sender
        for (Map.Entry<Integer, ClientHandler> clientEntry : connectedClients.entrySet()) {
            int recipientUserId = clientEntry.getKey();
            ClientHandler recipientHandler = clientEntry.getValue();
            
            // Don't send message back to the sender
            if (recipientUserId != senderUserId) {
                recipientHandler.sendMessageToClient(formattedMessage);
            }
        }
        
        System.out.println("Message broadcasted from User " + senderUserId + 
                          " (" + senderUsername + "): " + messageContent);
    }
    
    /**
     * Removes a client from the server when they disconnect
     * @param userId The user ID of the disconnecting client
     * @param username The username of the disconnecting client
     */
    public void removeDisconnectedClient(int userId, String username) {
        connectedClients.remove(userId);
        activeUsernames.remove(username);
        
        System.out.println("Client disconnected - User ID: " + userId + 
                          " (" + username + ") | Remaining clients: " + connectedClients.size());
        
        // Notify other clients about the disconnection
        String disconnectionMessage = "*** " + username + " has left the chat ***";
        for (ClientHandler clientHandler : connectedClients.values()) {
            clientHandler.sendMessageToClient(disconnectionMessage);
        }
    }
    
    /**
     * Checks if a username is already taken by another client
     * @param proposedUsername The username to check
     * @return true if username is available, false if taken
     */
    public synchronized boolean isUsernameAvailable(String proposedUsername) {
        return !activeUsernames.contains(proposedUsername);
    }
    
    /**
     * Registers a username as taken by a client
     * @param username The username to register
     */
    public synchronized void registerUsername(String username) {
        activeUsernames.add(username);
    }
    
    /**
     * Announces when a new user joins the chat
     * @param username The username of the new user
     */
    public void announceNewUserJoined(String username) {
        String joinMessage = "*** " + username + " has joined the chat ***";
        for (ClientHandler clientHandler : connectedClients.values()) {
            clientHandler.sendMessageToClient(joinMessage);
        }
    }
    
    /**
     * Gracefully shuts down the server and closes all connections
     */
    public void shutdownServer() {
        isServerRunning = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        // Close all client connections
        for (ClientHandler clientHandler : connectedClients.values()) {
            clientHandler.closeClientConnection();
        }
        
        clientThreadPool.shutdown();
        System.out.println("Chat server has been shut down.");
    }
    
    /**
     * Main method to start the chat server
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        
        // Add shutdown hook for graceful server termination
        Runtime.getRuntime().addShutdownHook(new Thread(chatServer::shutdownServer));
        
        chatServer.startServer();
    }
}

/**
 * ClientHandler class manages individual client connections and message processing
 * Each client gets its own ClientHandler thread for concurrent message handling
 */
class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final int userId;
    private final ChatServer parentServer;
    
    private BufferedReader messageReader;
    private PrintWriter messageWriter;
    private String clientUsername;
    private boolean isClientConnected;
    
    /**
     * Constructor for ClientHandler
     * @param clientSocket The socket connection to the client
     * @param userId Unique identifier for this client
     * @param parentServer Reference to the main chat server
     */
    public ClientHandler(Socket clientSocket, int userId, ChatServer parentServer) {
        this.clientSocket = clientSocket;
        this.userId = userId;
        this.parentServer = parentServer;
        this.isClientConnected = true;
    }
    
    /**
     * Main thread execution method - handles client communication
     */
    @Override
    public void run() {
        try {
            setupClientCommunication();
            handleUsernameSetup();
            processClientMessages();
        } catch (IOException e) {
            System.err.println("Client communication error for User " + userId + ": " + e.getMessage());
        } finally {
            closeClientConnection();
        }
    }
    
    /**
     * Sets up input/output streams for client communication
     * @throws IOException if stream setup fails
     */
    private void setupClientCommunication() throws IOException {
        messageReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        messageWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        
        // Send welcome message with user ID
        messageWriter.println("=== Welcome to the Chat Server ===");
        messageWriter.println("Your User ID is: " + userId);
        messageWriter.println("Please enter your username:");
    }
    
    /**
     * Handles the username setup process for new clients
     * @throws IOException if communication fails during username setup
     */
    private void handleUsernameSetup() throws IOException {
        while (isClientConnected) {
            String proposedUsername = messageReader.readLine();
            
            if (proposedUsername == null) {
                return; // Client disconnected
            }
            
            proposedUsername = proposedUsername.trim();
            
            if (proposedUsername.isEmpty()) {
                messageWriter.println("Username cannot be empty. Please try again:");
                continue;
            }
            
            if (parentServer.isUsernameAvailable(proposedUsername)) {
                clientUsername = proposedUsername;
                parentServer.registerUsername(clientUsername);
                messageWriter.println("Username accepted! You are now connected to the chat.");
                messageWriter.println("Type your messages below (type 'quit' to exit):");
                
                // Announce new user to existing clients
                parentServer.announceNewUserJoined(clientUsername);
                break;
            } else {
                messageWriter.println("Username '" + proposedUsername + "' is already taken. Please choose another:");
            }
        }
    }
    
    /**
     * Main message processing loop - handles incoming messages from client
     * @throws IOException if message reading fails
     */
    private void processClientMessages() throws IOException {
        String incomingMessage;
        
        while (isClientConnected && (incomingMessage = messageReader.readLine()) != null) {
            incomingMessage = incomingMessage.trim();
            
            // Handle quit command
            if (incomingMessage.equalsIgnoreCase("quit")) {
                messageWriter.println("Goodbye! You have been disconnected from the chat.");
                break;
            }
            
            // Ignore empty messages
            if (incomingMessage.isEmpty()) {
                continue;
            }
            
            // Broadcast message to all other clients
            parentServer.broadcastMessageToAllClients(userId, incomingMessage, clientUsername);
        }
    }
    
    /**
     * Sends a message to this specific client
     * @param message The message to send to the client
     */
    public void sendMessageToClient(String message) {
        if (messageWriter != null && isClientConnected) {
            messageWriter.println(message);
        }
    }
    
    /**
     * Closes the client connection and cleans up resources
     */
    public void closeClientConnection() {
        isClientConnected = false;
        
        try {
            if (messageReader != null) {
                messageReader.close();
            }
            if (messageWriter != null) {
                messageWriter.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client connection for User " + userId + ": " + e.getMessage());
        }
        
        // Remove client from server's active client list
        if (clientUsername != null) {
            parentServer.removeDisconnectedClient(userId, clientUsername);
        }
    }
}
