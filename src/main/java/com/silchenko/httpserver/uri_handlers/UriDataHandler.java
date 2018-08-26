package com.silchenko.httpserver.uri_handlers;


import com.silchenko.httpserver.HistoryHolder;
import com.silchenko.httpserver.Mapped;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

@Mapped(uri = "/data")
public class UriDataHandler extends UriHandlerBase {
    private final HistoryHolder historyHolder;

    public UriDataHandler(HistoryHolder historyHolder) {
        this.historyHolder = historyHolder;
    }

    @Override
    public void process(FullHttpRequest request, StringBuilder buff) {
        String method = request.method().name();
        switch (method) {
            case "GET":
                doGet(request, buff);
                break;
            case "POST":
                doPost(request, buff);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void doGet(FullHttpRequest request, StringBuilder buff) {
        Map<String, List<String>> params = new QueryStringDecoder(request.uri()).parameters();
    }

    private void doPost(FullHttpRequest request, StringBuilder buff) {
        buff.append(request.content());
        if (request.headers().get("Content-type").equalsIgnoreCase(getContentType())) {
            String json = request.content().toString(CharsetUtil.UTF_8);
            if (!json.isEmpty()) {
                historyHolder.addRecord(request.content().toString(CharsetUtil.UTF_8));
                buff.append(historyHolder.getHistory());
            }
        }
    }
}
