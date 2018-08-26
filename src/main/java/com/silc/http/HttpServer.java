package com.silc.http;

import com.silc.http.uri_handlers.UriHandlerBase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.List;

public class HttpServer {

    private enum State {
        NOT_STARTED,
        RUNNING,
        FAILED
    }

    private final int port;
    private Enum state;
    private final List<? extends UriHandlerBase> handlers;

    public HttpServer(List<? extends UriHandlerBase> handlers) {
        this.port = 9999;
        this.handlers = handlers;
        this.state = State.NOT_STARTED;
    }

    public void start() throws Exception {
        if (state == State.RUNNING) {
            return;
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerInitializer())
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Channel ch = b.bind(port).sync().channel();
            state = State.RUNNING;
            ch.closeFuture().sync();
        } finally {
            state = State.FAILED;
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new HttpServerCodec());
            ch.pipeline().addLast("aggregator", new HttpObjectAggregator(1048576));
            ch.pipeline().addLast("serverHandler", new UriHandler(handlers));
        }
    }
}
