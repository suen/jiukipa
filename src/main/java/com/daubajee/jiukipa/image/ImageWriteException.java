package com.daubajee.jiukipa.image;

public class ImageWriteException extends ApplicationException {

    private static final long serialVersionUID = 31786620383449557L;

    public ImageWriteException() {
    }

    public ImageWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageWriteException(String message) {
        super(message);
    }

    public ImageWriteException(Throwable cause) {
        super(cause);
    }

}
