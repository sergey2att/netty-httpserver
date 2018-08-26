package com.silc.http;


import com.silc.http.uri_handlers.UriHandlerBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.rtsp.RtspHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.rtsp.RtspResponseStatuses.BAD_REQUEST;

public class UriHandler extends SimpleChannelInboundHandler<Object> {

    private final StringBuilder buf = new StringBuilder();
    private Map<String, UriHandlerBase> handlers = new HashMap<>();

    public UriHandler(List<? extends UriHandlerBase> handlersH) {
        if (handlers.size() == 0) {
            handlersH.forEach(v -> {
                Annotation annotation = v.getClass().getAnnotation(Mapped.class);
                handlers.put(((Mapped) annotation).uri(), v);
            });
        }
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
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());

            if (!handlers.keySet().contains(queryStringDecoder.path())){
                throw new IllegalArgumentException(
                  "It works"
                );
            }

            buf.setLength(0);
            String context = queryStringDecoder.path();
            handler = handlers.get(context);
            if (handler != null) {
                handler.process(request, buf);
            }
           /* FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1,
                    ((LastHttpContent) msg).decoderResult().isSuccess() ? OK : BAD_REQUEST,
                    Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8)
            );
            response.headers().set(CONTENT_TYPE, handler != null ? handler.getContentType() : "text/plain; charset=UTF-8");
            ctx.writeAndFlush(response);*/
            writeResponse(request, ctx, handler);

            //  sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.ACCEPTED));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        throw new RuntimeException(cause);
    }

    private void writeResponse(FullHttpRequest msg, ChannelHandlerContext ctx, UriHandlerBase handler) {


        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, msg.decoderResult().isSuccess()
                ? OK : BAD_REQUEST, Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8)

        );
        response.headers().set(CONTENT_TYPE, handler != null ? handler.getContentType() : "text/plain; charset=UTF-8");

        /*if (isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        }*/

        // Write the response.
        ctx.write(response);

        //   if (!keepAlive)  {
        // If keep-alive is off, close the connection once the content is fully written.
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        //  } else {
        //     ctx.flush();
        // }
    }


    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
