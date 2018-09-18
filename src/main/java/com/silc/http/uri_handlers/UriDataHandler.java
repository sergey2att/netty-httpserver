package com.silc.http.uri_handlers;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.silc.http.Mapped;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mapped(uri = "/data")
public class UriDataHandler extends UriHandlerBase {
    private final ObservableList<String> observableList;

    public UriDataHandler(ObservableList<String> observableList) {
        this.observableList = observableList;
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
                throw new UnsupportedOperationException("HttpRequest is not supported: " + request.method().name());
        }
    }

    private void doGet(FullHttpRequest request, StringBuilder buff) {
        List<String> filterResult = new ArrayList<>();
        Map<String, List<String>> params = new QueryStringDecoder(request.uri()).parameters();
        if (!params.isEmpty()) {
            String timestamp = parseSingleParameter(params, "timestamp");
            String path = parseSingleParameter(params, "path");
            String value = parseSingleParameter(params, "value");

            if (!StringUtils.isBlank(path) && !StringUtils.isBlank(value)) {
                Predicate<String> filter = data -> parseValueByPath(data, path).equals(value);

                String res = HandlerUtils.find(observableList, v -> {
                    if (!StringUtils.isBlank(timestamp)) {
                        if (parseTimeStamp(v) < Long.parseLong(timestamp))
                            return false;
                    }
                    return filter.test(v);
                }, 10000);

                filterResult = Optional.ofNullable(res)
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            }
        }
        buff.append(filterResult.stream().collect(Collectors.joining(System.lineSeparator())));
    }

    @Nullable
    private String parseSingleParameter(Map<String, List<String>> params, String name) {
        return Optional.ofNullable(params.get(name)).map(v -> v.get(0)).orElse(null);
    }

    private String parseValueByPath(String data, String path) {
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
        return JsonPath.read(record, "$.systemDetails.clientTimestamp");
    }

    private void doPost(FullHttpRequest request, StringBuilder buff) {
        String content = request.content().toString(CharsetUtil.UTF_8);
        buff.append(content);
        if (request.headers().get(HttpHeaderNames.CONTENT_TYPE).equals(MediaType.APPLICATION_JSON) &&
                !content.isEmpty()) {
            observableList.add(content);
        }
    }
}
