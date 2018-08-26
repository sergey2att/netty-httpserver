package com.silc.http.uri_handlers;


import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.silc.http.HistoryHolder;
import com.silc.http.Mapped;
import com.sun.istack.internal.Nullable;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        HistoryHolder res = historyHolder;

        Map<String, List<String>> params = new QueryStringDecoder(request.uri()).parameters();
        if (!params.isEmpty()) {
            String timestamp = parseSingleParameter(params, "timestamp");
            String path = parseSingleParameter(params, "path");
            String value = parseSingleParameter(params, "value");
            if (Strings.isNullOrEmpty(timestamp)) {
                res.setHistory(applyTimestamp(res.getHistory(), Long.parseLong(timestamp)));
            }
        }
    }

    @Nullable
    private String parseSingleParameter(Map<String, List<String>> params, String name) {
        return Optional.ofNullable(params.get(name)).map(v -> v.get(0)).orElse(null);

    }

    private List<String> applyTimestamp(List<String> data, long timestamp) {
        return data.stream()
                .filter(v->parseTimeStamp(v) > timestamp)
                .collect(Collectors.toList());
    }

    private long parseTimeStamp(String record) {
       return new Gson().fromJson(record, JsonObject.class)
                .getAsJsonObject("systemDetails")
                .get("clientTimestamp")
                .getAsLong();
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
