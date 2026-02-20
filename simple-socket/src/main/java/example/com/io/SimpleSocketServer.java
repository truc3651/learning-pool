package example.com.io;

import java.io.*;
import java.net.*;

/**
 * This server demonstrates the fundamental socket concepts:
 *   1. ServerSocket — the "listening phone" that waits for incoming calls
 *   2. Socket — the actual connection once someone "picks up"
 *   3. InputStream/OutputStream — reading and writing data through the connection
 *
 * This is BLOCKING I/O (BIO) — the traditional approach.
 * Every operation (accept, read, write) BLOCKS the thread until it completes.
 */
public class SimpleSocketServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(9090);
        System.out.println("Server listening on port 9090...");
        System.out.println("(Waiting for a client to connect)\n");
        // accept() BLOCKS here — the thread goes to sleep and waits.
        // It won't continue until a client actually connects.
        // When a client connects, accept() returns a NEW Socket object.
        // The ServerSocket only allows 1 client, because accept() never call again
        Socket clientSocket = serverSocket.accept();

        // At this point, the TCP 3-way handshake has completed.
        // We have a live connection!
        System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());
        System.out.println("  Client's address: " + clientSocket.getInetAddress());
        System.out.println("  Client's port:    " + clientSocket.getPort());
        System.out.println("  Our local port:   " + clientSocket.getLocalPort());
        System.out.println();

        BufferedReader fromClient = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
        );
        PrintWriter toClient = new PrintWriter(
                clientSocket.getOutputStream(), true
        );
        toClient.println("Hello! I'm the server. Type messages (type 'bye' to quit):");

        String message;
        while ((message = fromClient.readLine()) != null) {
            // readLine() BLOCKS until the client sends a complete line.
            System.out.println("Client says: " + message);
            if ("quit".equalsIgnoreCase(message.trim())) {
                toClient.println("Goodbye!");
                break;
            }
            toClient.println("Server received: " + message);
        }

        System.out.println("Client disconnected. Shutting down.");
        clientSocket.close();
        serverSocket.close();
    }
}