package com.daubajee.jiukipa.image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.IntStream;

import com.daubajee.jiukipa.EventTopics;
import com.daubajee.jiukipa.batch.Config;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ImageStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageStorage.class);

    private JpegEXIFExtractor jpegEXIFExtractor = new JpegEXIFExtractor();

    private Config config;

    private EventBus eventBus;

    public ImageStorage(Config config, EventBus eventBus) {
        this.config = config;
        this.eventBus = eventBus;
        init();
    }

    private void init() {
        String imageRepoHome = getImageRepoHome();
        int numberOfParitions = getNumberOfPartitions();
        String partitionPrefix = getPartitionPrefix();

        IntStream.range(1, numberOfParitions + 1)
                .forEach(num -> createDirIfNotExists(Paths.get(imageRepoHome, partitionPrefix + num)));
    }

    private String getPartitionPrefix() {
        return config.partitionPrefix();
    }

    private String getImageRepoHome() {
        return config.imageRepoHome();
    }

    private int getNumberOfPartitions() {
        return config.numberOfParition();
    }

    private void createDirIfNotExists(Path path) {
        File dir = path.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(path.toString() + " not a directory");
        }
    }

    public void addNewImage(byte[] jpegImageBytes) {
        HashCode hashSHA256 = toHashSHA256(jpegImageBytes);

        try {
            Map<String, String> metadata = extractMetaData(jpegImageBytes);
            addNewImage(hashSHA256, jpegImageBytes, metadata);
        } catch (ImageMetadataExtractionException e) {
            throw new ImageWriteException("Write failed for : " + hashSHA256.toString(), e);
        }
    }

    public void addNewImage(HashCode imageHash, byte[] jpegImageBytes, Map<String, String> metadata) {

        String widthStr = getAttr(metadata, "tiff:ImageWidth");
        String heightStr = getAttr(metadata, "tiff:ImageLength");

        int exifWidth = Integer.parseInt(widthStr);
        int exifHeight = Integer.parseInt(heightStr);

        Path imagePath = addImageWithSize(imageHash, jpegImageBytes, exifWidth, exifHeight);
        Path metadataPath = addImagaMetaData(imageHash, metadata);

        eventBus.publish(EventTopics.NEW_IMAGE, metadataPath.toString());

        LOGGER.info("Image and metadata added : " + imagePath.toString() + ", " + metadataPath.toString());
    }

    public Path addImageWithSize(HashCode imageHash, byte[] jpegImageBytes, int width, int height) {
        String widthXheight = String.format("%sx%s", width, height);
        int selectedPartition = selectPartition(getNumberOfPartitions(), imageHash);
        String imageFileName = imageHash.toString() + "_" + widthXheight + ".JPG";
        String partitionDir = getPartitionPrefix() + selectedPartition;
        Path imagePath = Paths.get(getImageRepoHome(), partitionDir, imageFileName);

        File imageFile = imagePath.toFile();
        if (imageFile.exists()) {
            throw new ImageAlreadyExistsException(
                    imageFileName + " already exists at " + imagePath.getParent().toString());
        }
        try {
            return Files.write(imagePath, jpegImageBytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ImageWriteException("Write failed for : " + imagePath.toString(), e);
        }
    }

    public static HashCode toHashSHA256(byte[] bytes) {
        HashFunction sha256 = Hashing.sha256();
        HashCode hashBytes = sha256.hashBytes(bytes);
        return hashBytes;
    }

    public Map<String, String> extractMetaData(byte[] jpegBytes) {
        try {
            return jpegEXIFExtractor.extract(new ByteArrayInputStream(jpegBytes));
        } catch (Exception e) {
            throw new ImageMetadataExtractionException("Metadata extraction failed ", e);
        }
    }

    private Path addImagaMetaData(HashCode imageHash, Map<String, String> metadata) {
        int selectedPartition = selectPartition(getNumberOfPartitions(), imageHash);
        String partitionDir = getPartitionPrefix() + selectedPartition;
        Path metaFilePath = Paths.get(getImageRepoHome(), partitionDir, imageHash.toString() + ".meta");
        byte[] jsonBytes = getJsonBytes(metadata);

        try {
            return Files.write(metaFilePath, jsonBytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ImageWriteException("Write failed for : " + metaFilePath.toString(), e);
        }
    }

    private static byte[] getJsonBytes(Map<String, String> metadata) {
        JsonObject json = new JsonObject();
        metadata.entrySet().forEach(entry -> json.put(entry.getKey(), entry.getValue()));
        return json.toString().getBytes();
    }

    private static String getAttr(Map<String, String> imageMetadata, String attr) {
        String attrVal = imageMetadata.getOrDefault(attr, "");
        if (attrVal.isEmpty()) {
            throw new IllegalArgumentException("Metadata does not contain " + attrVal);
        }
        return attrVal;
    }

    public static int selectPartition(int numberOfPartitions, HashCode hashcode) {
        BigInteger numberOfNodes = new BigInteger(ByteBuffer.allocate(4).putInt(numberOfPartitions).array());
        BigInteger bigInteger = new BigInteger(hashcode.asBytes());
        int mod = bigInteger.mod(numberOfNodes).intValue();
        return mod + 1;
    }


}
