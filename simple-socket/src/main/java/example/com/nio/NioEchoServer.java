package example.com.nio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * NIO introduced THREE new concepts that replaced the old I/O:
 *
 *   OLD (java.io)              NEW (java.nio)
 *   ────────────────           ────────────────
 *   Socket                  →  SocketChannel
 *   ServerSocket             →  ServerSocketChannel
 *   InputStream/OutputStream →  ByteBuffer
 *   (nothing equivalent)     →  Selector ← THIS IS THE KEY INNOVATION
 *
 * THE CORE IDEA:
 * Instead of one thread per connection (blocking on each socket),
 * we use ONE thread with a Selector that monitors ALL sockets.
 * The Selector tells us: "Hey, socket #47 has data ready to read,
 * and socket #102 is ready to accept a write."
 */
public class NioEchoServer {
    public static void main(String[] args) throws IOException {
        // A Channel is like a Socket but can be set to NON-BLOCKING mode.
        // In non-blocking mode, read() and write() return IMMEDIATELY
        // even if there's no data — they never put the thread to sleep.
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(9090));
        serverChannel.configureBlocking(false);  // ← THIS IS CRITICAL

        // The Selector is the magic ingredient that makes NIO work.
        // You register channels with it and tell it what events you
        // care about. Then you call select() which efficiently waits
        // until at least one registered channel has an event ready.
        //
        // Under the hood, the Selector uses OS-specific mechanisms:
        //   Linux: epoll (very efficient)
        //   macOS: kqueue
        Selector selector = Selector.open();

        // OP_ACCEPT = "tell me when a new client wants to connect"
        // OP_CONNECT = connection to remote server completed (client-side)
        // OP_READ    = data is available to read
        // OP_WRITE   = the channel is ready to accept writes
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        int connectionCount = 0;
        while (true) {
            // select() blocks until at least one channel has an event.
            // But it's NOT like blocking in BIO! Here, ONE call to select()
            // can wake up for ANY of the thousands of registered channels.
            // In BIO, each blocking read() only watches ONE socket.
            int readyCount = selector.select();

            if (readyCount == 0) continue;

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();  // MUST remove — otherwise we'd process it again

                try {
                    if (key.isAcceptable()) {
                        // ──── A new client wants to connect ────
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);

                        // Register this new client channel for READ events.
                        // Now the selector will also watch this channel!
                        // The attachment (ByteBuffer) is per-connection state.
                        clientChannel.register(selector, SelectionKey.OP_READ,
                                ByteBuffer.allocate(1024));

                        connectionCount++;
                        System.out.println("Client #" + connectionCount + " connected: "
                                + clientChannel.getRemoteAddress());

                        // Send welcome message
                        clientChannel.write(ByteBuffer.wrap(
                                ("Welcome! You are client #" + connectionCount + "\r\n").getBytes()
                        ));
                    }

                    if (key.isReadable()) {
                        // ──── A client has sent data ────
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        // Returns number of bytes read, or -1 if client disconnected.
                        int bytesRead = clientChannel.read(buffer);
                        if (bytesRead == -1) {
                            System.out.println("Client disconnected: "
                                    + clientChannel.getRemoteAddress());
                            key.cancel();
                            clientChannel.close();
                            continue;
                        }

                        // ── The dreaded flip() ──
                        buffer.flip();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        String message = new String(data).trim();

                        System.out.println("Received: " + message);
                        String response = "Echo: " + message + "\r\n";
                        clientChannel.write(ByteBuffer.wrap(response.getBytes()));
                        buffer.clear();
                    }

                } catch (IOException e) {
                    System.err.println("Error: " + e.getMessage());
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }
}