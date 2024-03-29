package org.zaikorea.zaiclient.exceptions;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException() {
        super("The operation tried to delete a nonexistent item.");
    }
}
