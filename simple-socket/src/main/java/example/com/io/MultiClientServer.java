package example.com.io;

import java.io.*;
import java.net.*;

/**
 * MULTI-CLIENT SERVER — Shows the Thread-Per-Connection Problem
 *
 * The SimpleSocketServer could only handle ONE client at a time.
 * Why? Because after accept() returns one client, the thread enters
 * the while(readLine()) loop and is STUCK there until that client
 * disconnects. No one else can connect.
 *
 * The "solution" in the BIO world: create a NEW THREAD for each client.
 * This works... until you have 10,000 clients and 10,000 threads.
 */
public class MultiClientServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(9090);
        System.out.println("Multi-Client Server listening on port 9090...");
        System.out.println("Each client gets its own dedicated thread.\n");

        int clientCount = 0;
        while (true) {
            // BLOCKS until a client connects
            Socket clientSocket = serverSocket.accept();
            clientCount++;

            int clientId = clientCount;
            System.out.println("Client #" + clientId + " connected from "
                    + clientSocket.getRemoteSocketAddress());
            System.out.println("  Active threads: " + Thread.activeCount());

            // ════════════════════════════════════════════════════════
            // HERE IS THE CORE PATTERN (AND PROBLEM):
            // We MUST create a new thread for each client.
            // Without this, accepting client #2 would be impossible
            // while we're reading from client #1.
            // ════════════════════════════════════════════════════════
            Thread clientThread = new Thread(() -> {
                try {
                    handleClient(clientSocket, clientId);
                } catch (IOException e) {
                    System.err.println("Error with client #" + clientId + ": " + e.getMessage());
                }
            }, "client-handler-" + clientId);

            clientThread.start();
            // Now the main thread loops back to accept() to wait for the next client
        }
    }

    // Handle one client on a dedicated thread.
    private static void handleClient(Socket socket, int clientId) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println("Welcome, you are client #" + clientId);

        String message;
        while ((message = in.readLine()) != null) {
            // ↑↑↑ THIS LINE IS WHERE THE THREAD BLOCKS ↑↑↑
            // The thread sleeps here until the client sends data.
            // For a chat application where users type slowly,
            // this thread is idle 99.9% of the time.

            System.out.println("[Client #" + clientId + "] " + message);

            if ("quit".equalsIgnoreCase(message.trim())) {
                out.println("Goodbye, client #" + clientId + "!");
                break;
            }

            out.println("Echo to #" + clientId + ": " + message);
        }

        socket.close();
        System.out.println("Client #" + clientId + " disconnected. "
                + "Active threads: " + Thread.activeCount());
    }
}