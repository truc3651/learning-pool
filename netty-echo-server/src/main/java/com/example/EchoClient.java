package com.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class EchoClient {
    private final String host;
    private final int port;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        // Client only needs one EventLoopGroup (no boss/worker split)
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))
                            .addLast(new StringDecoder())
                            .addLast(new StringEncoder())
                            .addLast(new EchoClientHandler());
                    }
                });
            ChannelFuture future = bootstrap.connect(host, port).sync();
            System.out.println("Connected to " + host + ":" + port);
            System.out.println("Type messages and press Enter. Type 'quit' to exit.\n");

            Channel channel = future.channel();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null) {
                channel.writeAndFlush(line + "\r\n");
                if ("quit".equalsIgnoreCase(line.trim())) {
                    // Give server time to respond before closing
                    Thread.sleep(500);
                    break;
                }
            }
            channel.closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new EchoClient("localhost", 8080).start();
    }
}
