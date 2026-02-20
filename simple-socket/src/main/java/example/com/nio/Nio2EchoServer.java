package example.com.nio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NIO.1 (Java 1.4): YOU poll for events with a Selector ("are you ready yet?")
 * NIO.2 (Java 7):    OS NOTIFIES you when operations complete ("hey, it's done!")
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  COMPARISON OF ALL THREE APPROACHES                         │
 * │                                                             │
 * │  BIO (java.io):                                             │
 * │    socket.read()  → thread BLOCKS until data arrives        │
 * │                                                             │
 * │  NIO.1 (java.nio):                                          │
 * │    selector.select() → thread blocks until ANY channel      │
 * │                         has an event                        │
 * │    channel.read()    → returns immediately (non-blocking)   │
 * │    YOU write the event loop                                 │
 * │                                                             │
 * │  NIO.2 (java.nio, Java 7+):                                 │
 * │    channel.read(buffer, attachment, callback)               │
 * │    → returns immediately, OS calls your callback when done  │
 * │    NO event loop needed — the OS manages everything         │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Linux historically had POOR true async I/O:
 *   - POSIX AIO: Uses a thread pool internally to simulate async (it's FAKE — just BIO hidden behind an async API!)
 *   - Linux native AIO (io_submit): Only works for direct I/O on files, NOT for sockets
 *   - io_uring (Linux 5.1+, 2019): Finally a true async interface, but Java NIO.2 was designed in 2011 and doesn't use it
 *
 * So on Linux, Java NIO.2 typically simulates async I/O using an INTERNAL THREAD POOL that
 * makes blocking epoll/read calls behind the scenes! Your code LOOKS async, but underneath
 * there are hidden threads doing blocking I/O.
 */
public class Nio2EchoServer {
    private static final AtomicInteger connectionCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        AsynchronousServerSocketChannel serverChannel =
                AsynchronousServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(9090));
        // The accept() call returns IMMEDIATELY. When a client connects,
        // the OS invokes the CompletionHandler.completed() method.
        startAccepting(serverChannel);

        // Keep the main thread alive (the real work happens in callbacks)
        // In NIO.1, the main thread ran the event loop.
        // In NIO.2, the main thread has nothing to do — callbacks are
        // invoked by the internal thread pool.
        System.out.println("Main thread is FREE — all work happens in callbacks.");
        System.out.println("Press Enter to stop the server.\n");
        System.in.read();

        serverChannel.close();
    }

    private static void startAccepting(AsynchronousServerSocketChannel serverChannel) {
        // attachment: any object you want passed to the callback (like a cookie)
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            // Called BY THE OS when a client connects
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                int clientId = connectionCount.incrementAndGet();
                try {
                    System.out.println("Client #" + clientId + " connected: "
                            + clientChannel.getRemoteAddress());
                    System.out.println("  Callback thread: " + Thread.currentThread().getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // IMPORTANT: Start accepting the NEXT connection immediately!
                // Without this, the server would only accept one client.
                startAccepting(serverChannel);
                handleClientAsync(clientChannel, clientId);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("Accept failed: " + exc.getMessage());
            }
        });
    }

    private static void handleClientAsync(AsynchronousSocketChannel channel, int clientId) {
        String welcome = "Welcome! You are client #" + clientId + "\r\n";
        ByteBuffer welcomeBuffer = ByteBuffer.wrap(welcome.getBytes());
        channel.write(welcomeBuffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesWritten, Void attachment) {
                // Welcome sent! Now start reading from this client.
                startReading(channel, clientId);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("Write failed for client #" + clientId);
                try { channel.close(); } catch (IOException e) {}
            }
        });
    }

    private static void startReading(AsynchronousSocketChannel channel, int clientId) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer bytesRead, ByteBuffer buf) {
                if (bytesRead == -1) {
                    System.out.println("Client #" + clientId + " disconnected.");
                    try { channel.close(); } catch (IOException e) {}
                    return;
                }

                buf.flip();
                byte[] data = new byte[buf.remaining()];
                buf.get(data);
                String message = new String(data).trim();

                System.out.println("[Client #" + clientId + "] " + message
                        + "  (on thread: " + Thread.currentThread().getName() + ")");

                String response = "Echo: " + message + "\r\n";
                ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());

                channel.write(responseBuffer, null, new CompletionHandler<Integer, Void>() {
                    @Override
                    public void completed(Integer bytesWritten, Void v) {
                        // Write completed! Start reading the next message.
                        startReading(channel, clientId);
                    }

                    @Override
                    public void failed(Throwable exc, Void v) {
                        try { channel.close(); } catch (IOException e) {}
                    }
                });
            }

            @Override
            public void failed(Throwable exc, ByteBuffer buf) {
                System.err.println("Read failed for client #" + clientId);
                try { channel.close(); } catch (IOException e) {}
            }
        });
    }
}
