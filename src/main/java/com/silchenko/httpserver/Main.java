package com.silchenko.httpserver;

public class Main {

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8888;
        }
        new HttpServer(port).run();
    }
}
