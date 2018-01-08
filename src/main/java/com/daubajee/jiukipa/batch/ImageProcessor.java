package com.daubajee.jiukipa.batch;

import java.io.ByteArrayInputStream;
import java.util.Map;

import com.daubajee.jiukipa.image.JpegEXIFExtractor;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class ImageProcessor {

    JpegEXIFExtractor jpegEXIFExtractor = new JpegEXIFExtractor();

    public Map<String, String> getMetaData(byte[] jpegBytes) throws Exception {
        Map<String, String> metadata = jpegEXIFExtractor
                .extract(new ByteArrayInputStream(jpegBytes));
        return metadata;
    }

    public HashCode getSha256(byte[] bytes) {
        HashFunction sha256 = Hashing.sha256();
        HashCode hashBytes = sha256.hashBytes(bytes);
        return hashBytes;
    }

}
