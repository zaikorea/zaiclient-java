package org.zaikorea.ZaiClient.exceptions;

public class ItemSizeLimitExceededException extends Exception {

    public ItemSizeLimitExceededException() {
        super("Exceed max size of request item list.");
    }

}
