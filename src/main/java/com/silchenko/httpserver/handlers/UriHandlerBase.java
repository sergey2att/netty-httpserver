package com.silchenko.httpserver.handlers;


import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

public abstract  class UriHandlerBase {

    public abstract void process(FullHttpRequest request, StringBuilder buff);

    public String getContentType() {
        return "application/json";
    }

}
