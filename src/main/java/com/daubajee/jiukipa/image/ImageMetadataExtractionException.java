package com.daubajee.jiukipa.image;

public class ImageMetadataExtractionException extends ApplicationException {

    private static final long serialVersionUID = 2861009302891543289L;

    public ImageMetadataExtractionException() {
        super();
    }

    public ImageMetadataExtractionException(String message, Throwable causedBy) {
        super(message, causedBy);
    }

    public ImageMetadataExtractionException(String message) {
        super(message);
    }

    public ImageMetadataExtractionException(Throwable causedBy) {
        super(causedBy);
    }

}
