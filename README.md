# Http server with Netty

Example: 

```
List<? extends UriHandlerBase> handlerBases = Arrays.asList(new UriDataHandler(new HistoryHolder()));
        new HttpServer(handlerBases).start();
```
   
      