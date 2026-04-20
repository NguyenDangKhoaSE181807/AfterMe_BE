package com.example.reminder.exception;

public class DecryptDeniedException extends BadRequestException {

    private final DecryptDenyReason reason;

    public DecryptDeniedException(DecryptDenyReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public DecryptDenyReason getReason() {
        return reason;
    }
}
