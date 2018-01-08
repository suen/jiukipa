package com.daubajee.jiukipa.image;

public class ImageAlreadyExistsException extends ApplicationException {

    private static final long serialVersionUID = 2861009302891543289L;

    public ImageAlreadyExistsException() {
        super();
    }

    public ImageAlreadyExistsException(String message, Throwable causedBy) {
        super(message, causedBy);
    }

    public ImageAlreadyExistsException(String message) {
        super(message);
    }

    public ImageAlreadyExistsException(Throwable causedBy) {
        super(causedBy);
    }

}
