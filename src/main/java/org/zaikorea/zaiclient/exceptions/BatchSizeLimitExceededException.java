package org.zaikorea.zaiclient.exceptions;

public class BatchSizeLimitExceededException extends RuntimeException {

    public BatchSizeLimitExceededException() {
        super("The number of items in event batch exceeded the size limit.");
    }
}
