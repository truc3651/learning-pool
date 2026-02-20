package example.com.io;

import java.io.*;
import java.net.*;

public class SimpleSocketClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 9090);
        System.out.println("Connected!");
        System.out.println("  My local port: " + socket.getLocalPort());
        System.out.println();

        BufferedReader fromServer = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );
        PrintWriter toServer = new PrintWriter(
                socket.getOutputStream(), true
        );
        String serverGreeting = fromServer.readLine();
        System.out.println("Server: " + serverGreeting);

        BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in)
        );
        System.out.println("Type messages to send to server:\n");
        String input;
        while ((input = keyboard.readLine()) != null) {
            toServer.println(input);
            String response = fromServer.readLine();
            if (response == null) {
                System.out.println("Server closed the connection.");
                break;
            }
            System.out.println("Server: " + response);

            if ("quit".equalsIgnoreCase(input.trim())) {
                break;
            }
        }

        socket.close();
        System.out.println("Disconnected.");
    }
}
