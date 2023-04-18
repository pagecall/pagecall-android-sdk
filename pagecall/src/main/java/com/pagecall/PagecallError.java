package com.pagecall;

/**
 * Public API of Pagecall.
 * All Pagecall Exception will be delivered via {@link PagecallError}.
 */
public class PagecallError extends Exception {

    public PagecallError(String message) {
        super(message);
    }
}