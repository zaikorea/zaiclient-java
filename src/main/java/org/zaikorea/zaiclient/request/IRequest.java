package org.zaikorea.zaiclient.request;

public interface IRequest<T> {
    public String getBaseUrl();

    public String getPath();

    public T getPayload();
}
