package com.silc.http;

import com.silc.http.uri_handlers.UriHandlerBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class HttpHandler extends SimpleChannelInboundHandler<Object> {

    private final StringBuilder buf = new StringBuilder();
    private final Map<String, UriHandlerBase> handlers = new HashMap<>();

    public HttpHandler(List<? extends UriHandlerBase> uriHandlers) {
        uriHandlers.forEach(v -> this.handlers.put(v.getClass().getAnnotation(Mapped.class).uri(), v));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        UriHandlerBase handler;
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            try {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
                String context = queryStringDecoder.path();
                if (!this.handlers.keySet().contains(context)) {
                    writeNotFound(ctx, request);
                    return;
                }
                buf.setLength(0);

                handler = this.handlers.get(context);
                handler.process(request, buf);
                writeResponse(ctx, request, handler);
            } finally {
                request.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private void writeNotFound(ChannelHandlerContext ctx, FullHttpRequest request) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(String.format("Url %s not found", request.uri()), CharsetUtil.UTF_8);
        writeResponse(ctx, request, NOT_FOUND, byteBuf, MediaType.TEXT_PLAIN);
    }

    private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest request, UriHandlerBase handler) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8);
        writeResponse(ctx, request, request.decoderResult().isSuccess() ? OK : HttpResponseStatus.BAD_REQUEST, byteBuf,
                handler != null ? handler.getContentType() : MediaType.TEXT_PLAIN);
    }

    private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status,
                               ByteBuf buf, CharSequence contentType) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf, false);
        ZonedDateTime dateTime = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        DefaultHttpHeaders headers = (DefaultHttpHeaders) response.headers();
        headers.set(HttpHeaderNames.DATE, dateTime.format(formatter));
        headers.set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0");
        headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
        headers.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        if (!keepAlive || response.status() != OK) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response, ctx.voidPromise());
        }
    }
}
