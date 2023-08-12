package org.zaikorea.zaiclient.request;

public interface IRequest<T> {
    public default String getBaseUrl() {
        throw new UnsupportedOperationException("Unimplemented method 'getBaseUrl'");
    };

    public default String getPath() {
        throw new UnsupportedOperationException("Unimplemented method 'getPath'");
    }

    public default String getPath(String clientId) {
        throw new UnsupportedOperationException("Unimplemented method 'getPath'");
    }

    public T getPayload();
}
