package com.silc.http.uri_handlers;


import io.netty.handler.codec.http.FullHttpRequest;

import javax.ws.rs.core.MediaType;

public abstract class UriHandlerBase {

    public abstract void process(FullHttpRequest request, StringBuilder buff);

    public String getContentType() {
        return MediaType.APPLICATION_JSON;
    }

}
