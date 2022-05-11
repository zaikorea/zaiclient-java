package org.zaikorea.ZaiClient.exceptions;

public class ItemNotFoundException extends Exception {

    public ItemNotFoundException() {
        super("The operation tried to delete a nonexistent item.");
    }
}
