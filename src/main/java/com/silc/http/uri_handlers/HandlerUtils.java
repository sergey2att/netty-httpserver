package com.silc.http.uri_handlers;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class HandlerUtils {

    @Nullable
    public static <T> T find(ObservableList<T> list, Predicate<T> filter, long awaitMillis) {
        AtomicReference<T> res = new AtomicReference<>();
        ListChangeListener<T> listener = c -> {
            while (res.get() == null && c.next()) {
                if (c.wasAdded()) {
                    for (T v : c.getAddedSubList()) {
                        if (res.get() != null)
                            break;
                        if (filter.test(v)) {
                            res.set(v);
                            break;
                        }
                    }
                }
            }
        };
        list.addListener(listener);
        ArrayList<T> copy = new ArrayList<>(list);
        //from newer to older
        for (int i = copy.size() - 1; i >= 0; i--) {
            T v = copy.get(i);
            if (res.get() != null)
                break;
            if (filter.test(v)) {
                res.set(v);
                break;
            }
        }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < awaitMillis && res.get() == null) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        list.removeListener(listener);
        return res.get();
    }
}
