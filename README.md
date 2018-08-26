# Http server with Netty

Example: 

code:: java

List<? extends UriHandlerBase> handlerBases = Arrays.asList(new UriDataHandler(new HistoryHolder()));
        new HttpServer(handlerBases).start();