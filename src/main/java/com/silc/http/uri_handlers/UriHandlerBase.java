package com.silc.http.uri_handlers;


import io.netty.handler.codec.http.FullHttpRequest;

public abstract class UriHandlerBase {

    public abstract void process(FullHttpRequest request, StringBuilder buff);

    public String getContentType() {
        return "application/json; charset=UTF-8";
    }

}
