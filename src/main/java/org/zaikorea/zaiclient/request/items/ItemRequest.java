package org.zaikorea.zaiclient.request.items;

import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.request.IRequest;

public class ItemRequest<T> implements IRequest<T> {
    private String baseUrl = Config.collectorApiEndPoint;
    private String path = Config.itemsApiPath;
    private T items;

    public ItemRequest(T items) {
        this.items = items;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPath() {
        return path;
    }

    @Override
    public T getPayload() {
        return items;
    }
}
