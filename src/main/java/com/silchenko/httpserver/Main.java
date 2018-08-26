package com.silchenko.httpserver;

import com.silchenko.httpserver.uri_handlers.UriDataHandler;
import com.silchenko.httpserver.uri_handlers.UriHandlerBase;

import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        List<? extends UriHandlerBase> handlerBases = Arrays.asList(new UriDataHandler(new HistoryHolder()));
        new HttpServer(handlerBases).start();
    }
}
