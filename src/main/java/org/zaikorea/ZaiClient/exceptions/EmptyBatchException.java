package org.zaikorea.ZaiClient.exceptions;

public class EmptyBatchException extends Exception {

    public EmptyBatchException() {
        super("Cannot log empty EventBatch object.");
    }
}
