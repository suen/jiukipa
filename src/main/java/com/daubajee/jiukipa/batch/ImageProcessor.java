package com.daubajee.jiukipa.batch;

import java.io.ByteArrayInputStream;
import java.util.Map;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class ImageProcessor {
    JpegEXIFExtractor jpegEXIFExtractor = new JpegEXIFExtractor();

    public void processImage(byte[] imageBuffer) throws Exception {
        Map<String, String> metadata = jpegEXIFExtractor
                .extract(new ByteArrayInputStream(imageBuffer));

        HashCode sha256 = getSha256(imageBuffer);
        metadata.put("hash", sha256.toString());
    }

    public HashCode getSha256(byte[] bytes) {
        HashFunction sha256 = Hashing.sha256();
        HashCode hashBytes = sha256.hashBytes(bytes);
        return hashBytes;
    }

}
