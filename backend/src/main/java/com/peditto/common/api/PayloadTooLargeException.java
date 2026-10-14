package com.peditto.common.api;

import java.io.IOException;

public final class PayloadTooLargeException extends IOException {
    public PayloadTooLargeException() {
        super("Request body limit exceeded");
    }
}
