package org.zaikorea.zaiclient.exceptions;

public class EmptyBatchException extends RuntimeException {

    public EmptyBatchException() {
        super("Cannot log empty EventBatch object.");
    }
}
