import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * ChatClient class handles client-side functionality for the chat application
 * Connects to the ChatServer and provides a text-based interface for messaging
 */
public class ChatClient {
    // Server connection configuration
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    
    // Client socket and communication streams
    private Socket serverSocket;
    private BufferedReader messageReader;
    private PrintWriter messageWriter;
    private Scanner userInputScanner;
    
    // Client status and identification
    private boolean isConnectedToServer;
    private String currentUsername;
    private boolean chatSessionStarted;
    
    /**
     * Constructor initializes client components
     */
    public ChatClient() {
        this.userInputScanner = new Scanner(System.in);
        this.isConnectedToServer = false;
        this.chatSessionStarted = false;
    }
    
    /**
     * Establishes connection to the chat server
     * @return true if connection successful, false otherwise
     */
    public boolean connectToServer() {
        try {
            System.out.println("=== Chat Client Starting ===");
            System.out.println("Attempting to connect to server at " + SERVER_HOST + ":" + SERVER_PORT);
            
            // Create socket connection to server
            serverSocket = new Socket(SERVER_HOST, SERVER_PORT);
            
            // Set up communication streams
            messageReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            messageWriter = new PrintWriter(serverSocket.getOutputStream(), true);
            
            isConnectedToServer = true;
            System.out.println("Successfully connected to chat server!\n");
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            System.err.println("Please make sure the server is running and try again.");
            return false;
        }
    }
    
    /**
     * Starts the main client application loop
     */
    public void startChatSession() {
        if (!connectToServer()) {
            return;
        }
        
        try {
            // Handle initial server communication (welcome message and username setup)
            handleInitialServerCommunication();
            
            // Start background thread to receive messages from server AFTER username setup
            Thread messageReceiver = new Thread(this::receiveMessagesFromServer);
            messageReceiver.setDaemon(true);
            messageReceiver.start();
            
            // Main message sending loop
            handleUserMessageInput();
            
        } catch (IOException e) {
            System.err.println("Communication error with server: " + e.getMessage());
        } finally {
            disconnectFromServer();
        }
    }
    
    /**
     * Handles the initial communication with server including username setup
     * @throws IOException if communication with server fails
     */
    private void handleInitialServerCommunication() throws IOException {
        String serverMessage;
        
        // Read and display initial server messages
        while ((serverMessage = messageReader.readLine()) != null) {
            System.out.println(serverMessage);
            
            // Check if server is asking for username
            if (serverMessage.contains("Please enter your username:") || 
                serverMessage.contains("Please choose another:") ||
                serverMessage.contains("Please try again:")) {
                
                // Get username from user
                System.out.print("> ");
                String usernameInput = userInputScanner.nextLine().trim();
                currentUsername = usernameInput;
                messageWriter.println(usernameInput);
                
            } else if (serverMessage.contains("Username accepted!")) {
                // Read the next message which should be "Type your messages below"
                String nextMessage = messageReader.readLine();
                if (nextMessage != null) {
                    System.out.println(nextMessage);
                }
                chatSessionStarted = true;
                break;
            }
        }
    }
    
    /**
     * Handles user input for sending messages to the chat
     */
    private void handleUserMessageInput() {
        System.out.println("\n=== Chat Session Started ===");
        System.out.println("You can now start chatting! Type 'quit' to exit.\n");
        
        String userMessage;
        
        // Main input loop for user messages
        while (isConnectedToServer && chatSessionStarted) {
            System.out.print("> ");
            userMessage = userInputScanner.nextLine();
            
            if (userMessage == null) {
                break;
            }
            
            // Don't send empty messages
            if (userMessage.trim().isEmpty()) {
                continue;
            }
            
            // Check if user wants to quit
            if (userMessage.trim().equalsIgnoreCase("quit")) {
                messageWriter.println(userMessage);
                break;
            }
            
            // Send message to server
            messageWriter.println(userMessage);
            System.out.println("✓ Message sent: " + userMessage);
        }
    }
    
    /**
     * Background thread method to continuously receive messages from server
     * This runs concurrently with user input handling
     */
    private void receiveMessagesFromServer() {
        try {
            String incomingMessage;
            
            while (isConnectedToServer && (incomingMessage = messageReader.readLine()) != null) {
                
                // Only process messages after chat session has started
                if (!chatSessionStarted) {
                    continue;
                }
                
                // Skip server setup messages that might still come through
                if (incomingMessage.contains("Welcome to the Chat Server") ||
                    incomingMessage.contains("Your User ID is") ||
                    incomingMessage.contains("Please enter your username") ||
                    incomingMessage.contains("Username accepted") ||
                    incomingMessage.contains("Type your messages below")) {
                    continue;
                }
                
                // Display all other messages (chat messages and notifications)
                System.out.println();
                System.out.println("📨 " + incomingMessage);
                System.out.print("> ");
                System.out.flush();
            }
            
        } catch (IOException e) {
            if (isConnectedToServer) {
                System.err.println("\nConnection to server lost: " + e.getMessage());
            }
        }
    }
    
    /**
     * Cleanly disconnects from the server and closes all resources
     */
    public void disconnectFromServer() {
        isConnectedToServer = false;
        chatSessionStarted = false;
        
        try {
            if (messageWriter != null) {
                messageWriter.close();
            }
            if (messageReader != null) {
                messageReader.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error during disconnection: " + e.getMessage());
        }
        
        if (userInputScanner != null) {
            userInputScanner.close();
        }
        
        System.out.println("\nDisconnected from chat server. Goodbye!");
    }
    
    /**
     * Displays the client application header and instructions
     */
    private static void displayClientWelcomeMessage() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║          JAVA CHAT CLIENT            ║");
        System.out.println("║                                      ║");
        System.out.println("║  Instructions:                       ║");
        System.out.println("║  1. Enter a unique username          ║");
        System.out.println("║  2. Start chatting with others       ║");
        System.out.println("║  3. Type 'quit' to exit              ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
    }
    
    /**
     * Main method to start the chat client application
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        displayClientWelcomeMessage();
        
        ChatClient chatClient = new ChatClient();
        
        // Add shutdown hook for graceful client termination
        Runtime.getRuntime().addShutdownHook(new Thread(chatClient::disconnectFromServer));
        
        chatClient.startChatSession();
    }
}
