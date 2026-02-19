package com.example;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class EchoServerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("📥 Client connected: " + ctx.channel().remoteAddress());
        ctx.writeAndFlush("Welcome to the Netty Echo Server! Type something and press Enter.\r\n");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("📨 Received from " + ctx.channel().remoteAddress() + ": " + msg);
        if ("quit".equalsIgnoreCase(msg.trim())) {
            ctx.writeAndFlush("Goodbye!\r\n").addListener(future -> ctx.close());
            return;
        }
        String response = "Echo: " + msg + "\r\n";
        ctx.writeAndFlush(response);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("📤 Client disconnected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("❌ Error: " + cause.getMessage());
        ctx.close();
    }
}