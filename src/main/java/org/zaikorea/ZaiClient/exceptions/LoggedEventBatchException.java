package org.zaikorea.ZaiClient.exceptions;

public class LoggedEventBatchException extends Exception {

    public LoggedEventBatchException() {
        super("Cannot modify an already logged event batch.");
    }

}
