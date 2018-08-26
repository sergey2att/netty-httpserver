package com.silc.http.uri_handlers;


import com.google.common.base.Strings;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
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
        HistoryHolder res = new HistoryHolder();
        res.setHistory(historyHolder.getHistory());

        Map<String, List<String>> params = new QueryStringDecoder(request.uri()).parameters();
        if (!params.isEmpty()) {
            String timestamp = parseSingleParameter(params, "timestamp");
            String path = parseSingleParameter(params, "path");
            String value = parseSingleParameter(params, "value");
            if (!Strings.isNullOrEmpty(timestamp)) {
                res.setHistory(applyTimestamp(res.getHistory(), Long.parseLong(timestamp)));
            }
            if (!Strings.isNullOrEmpty(path) && !Strings.isNullOrEmpty(value)) {
                List<String> records = applyPathAndValue(res.getHistory(), path, value.replaceAll("\\+", " "));
                res.setHistory(records);
            }
        }
        buff.append(res.getHistory());
    }

    private List<String> applyPathAndValue(List<String> data, String path, String value) {
        return data.stream()
                .filter(v -> parseValueByPath(v, path).equals(value))
                .collect(Collectors.toList());
    }

    @Nullable
    private String parseSingleParameter(Map<String, List<String>> params, String name) {
        return Optional.ofNullable(params.get(name)).map(v -> v.get(0)).orElse(null);

    }

    private List<String> applyTimestamp(List<String> data, long timestamp) {
        return data.stream()
                .filter(v -> parseTimeStamp(v) > timestamp)
                .collect(Collectors.toList());
    }

    private String parseValueByPath(String data, String path) {
        path = path.startsWith("$") ? path : "$." + path;
        Object result = null;
        try {
            result = JsonPath.read(data, path);
        } catch (PathNotFoundException ignore) {
        }
        return Optional.ofNullable(result).map(r -> r.toString()
                .replaceAll("\"", "")
                .replace("[", "")
                .replace("]", ""))
                .orElse("");
    }

    private long parseTimeStamp(String record) {
        return Long.parseLong(JsonPath.read(record, "$.systemDetails.clientTimestamp"));
    }

    private void doPost(FullHttpRequest request, StringBuilder buff) {
        String content = request.content().toString(CharsetUtil.UTF_8);
        buff.append(content);
        if (request.headers().get("Content-type").equals("application/json")) {
            if (!content.isEmpty()) {
                historyHolder.addRecord(content);
            }
        }
    }
}
