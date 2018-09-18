package com.silc.http;

import com.silc.http.uri_handlers.UriDataHandler;
import com.silc.http.uri_handlers.UriHandlerBase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.EventExecutorGroup;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class HttpServer {

    public enum State {
        NOT_STARTED,
        RUNNING,
        STOPPED,
        FAILED
    }

    public static final int PORT = 9999;

    private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);
    private static final Lazy<HttpServer> INSTANCE = new Lazy<>(HttpServer::new);
    private static final ObservableList<String> history = FXCollections.observableArrayList();
    private static List<? extends UriHandlerBase> urlHandlers = Collections.singletonList(new UriDataHandler(history));

    private State state;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private HttpServer() {
        this.state = State.NOT_STARTED;
    }

    public static HttpServer getInstance() {
        return INSTANCE.getValue();
    }

    public static ObservableList<String> getHistory() {
        return history;
    }

    public State getState() {
        return this.state;
    }

    public void run() {
        if (state == State.RUNNING) {
            LOG.debug("Ignore start() call on sever since it has already been started");
            return;
        }
        LOG.debug("Starting sever...");
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerInitializer())
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            serverBootstrap.bind(PORT).sync().channel();
            state = State.RUNNING;
            LOG.debug("Sever has been started");
        } catch (InterruptedException e) {
            stop(State.FAILED);
            throw new RuntimeException("Sever thread has been interrupted: ", e);
        }
    }

    public synchronized void stop() {
        if (state != State.RUNNING) {
            LOG.debug("Server is not running ({})", state);
            return;
        }
        stop(State.STOPPED);
    }

    private synchronized void stop(State state) {
        LOG.debug("Stopping sever...");
        shoutDown(bossGroup, "bossGroup");
        bossGroup = null;
        shoutDown(workerGroup, "workerGroup");
        workerGroup = null;
        this.state = state;
        LOG.debug("Sever has been stopped with status {}", state);
    }

    private static void shoutDown(EventExecutorGroup group, String name) {
        if (group != null) {
            try {
                group.shutdownGracefully().await(5000);
                LOG.debug("{} has been stopped", name);
            } catch (InterruptedException e) {
                LOG.warn(name + " stopping error", e);
            }
        }
    }

    private class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new HttpServerCodec());
            ch.pipeline().addLast("aggregator", new HttpObjectAggregator(1048576));
            ch.pipeline().addLast("serverHandler", new HttpHandler(urlHandlers));
        }
    }
}
