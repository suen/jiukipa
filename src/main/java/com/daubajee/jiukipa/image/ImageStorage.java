package com.daubajee.jiukipa.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.daubajee.jiukipa.EventTopics;
import com.daubajee.jiukipa.batch.Config;
import com.daubajee.jiukipa.batch.ImageSize;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.coobird.thumbnailator.Thumbnails;

public class ImageStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageStorage.class);


    private JpegEXIFExtractor jpegEXIFExtractor = new JpegEXIFExtractor();

    final FileFilter filter = (File file) -> file.isDirectory()
            || file.getName().endsWith(".meta");

    private Config config;

    private EventBus eventBus;

    private Vertx vertx;

    public ImageStorage(Config config, Vertx vertx) {
        this.config = config;
        this.eventBus = vertx.eventBus();
        this.vertx = vertx;
        init();
    }

    private void init() {
        String imageRepoHome = getImageRepoHome();
        int numberOfParitions = getNumberOfPartitions();
        String partitionPrefix = getPartitionPrefix();

        IntStream.range(1, numberOfParitions + 1)
                .forEach(num -> createDirIfNotExists(Paths.get(imageRepoHome, partitionPrefix + num)));

        eventBus.consumer(EventTopics.REQUEST_REPLAY_IMAGE_META,
                message -> onReplayImageMeta(message));
    }

    private void onReplayImageMeta(Message<Object> message) {
        String imageRepoHome = getImageRepoHome();
        File imageRepoDir = new File(imageRepoHome);
        LOGGER.info("Directory processing requested : " + message.body());
        vertx.executeBlocking(future -> {
            List<String> imageMetaPaths = scanDirForImageMetas(imageRepoDir);
            LOGGER.info("Image Metas in the directory : " + imageMetaPaths.size());
            broadcastImageMetas(imageMetaPaths);
            future.complete();
        }, res -> {
            LOGGER.info("Directory processing done");
        });
    }

    private void processDirectory(File dir) {
        File[] files = dir.listFiles(filter);
        for (File file : files) {
            if (file.isFile()) {
                try {
                    JsonObject metaInfMap = extractImageMeta(file);
                    eventBus.publish(EventTopics.REPLAY_IMAGE_META, metaInfMap);
                } catch (IOException e) {
                    LOGGER.warn("Failed to extract meta file : " + file.getAbsolutePath(), e);
                }
            } else {
                processDirectory(file);
            }
        }
    }
    
    private List<String> scanDirForImageMetas(File dir){
        File[] files = dir.listFiles(filter);
        return Arrays.asList(files)
            .stream()
            .flatMap(file -> {
                if (file.isFile()) {
                    return Stream.of(file.getAbsolutePath());
                }
                return scanDirForImageMetas(file).stream();
            })
            .collect(Collectors.toList());
    }

    private void broadcastImageMetas(List<String> imageMetaPathStrs) {
        imageMetaPathStrs.forEach(imageMetaPathStr -> {
            File file = Paths.get(imageMetaPathStr).toFile();
            if (file.isFile()) {
                try {
                    JsonObject metaInfMap = extractImageMeta(file);
                    eventBus.publish(EventTopics.REPLAY_IMAGE_META, metaInfMap);
                } catch (IOException e) {
                    LOGGER.error("Failed to extract meta file : " + file.getAbsolutePath(), e);
                }
            } else {
                LOGGER.warn("Not a meta file : " + file.getAbsolutePath());
            }
        });
    }

    private static JsonObject extractImageMeta(File file)
            throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String str = new String(bytes, Charsets.UTF_8);
        JsonObject json = new JsonObject(str);
        String hashcode = file.getName().substring(0,
                file.getName().length() - 5);
        json.put("hashcode", hashcode);
        return json;
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

        Path imagePath = addImage(imageHash, jpegImageBytes);
        
        JsonObject json = toJson(metadata);
        json.put("hashcode", imageHash.toString());
        byte[] jsonBytes = json.toString().getBytes();
        
        addImagaMetaData(imageHash, jsonBytes);

        eventBus.publish(EventTopics.NEW_IMAGE_META, json);

        LOGGER.info("Image and metadata added : " + imagePath.toString());
    }

    public Path addImage(HashCode imageHash, byte[] jpegImageBytes) {
        int selectedPartition = selectPartition(getNumberOfPartitions(),
                imageHash);
        String imageFileName = imageHash.toString() + ".JPG";
        String partitionDir = getPartitionPrefix() + selectedPartition;
        Path imagePath = Paths.get(getImageRepoHome(), partitionDir,
                imageFileName);

        File imageFile = imagePath.toFile();
        if (imageFile.exists()) {
            throw new ImageAlreadyExistsException(imageFileName
                    + " already exists at " + imagePath.getParent().toString());
        }
        try {
            return Files.write(imagePath, jpegImageBytes,
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ImageWriteException(
                    "Write failed for : " + imagePath.toString(), e);
        }
    }

    public Path getImage(HashCode imageHash, int width, int height) {
        ImageSize stdSize = ImageSize.getLeastSmallestStdSize(width, height);
        return getImageByStdSize(imageHash, stdSize);
    }

    public Path getImageByStdSize(HashCode imageHash, String sizeName) {
        ImageSize stdSize = ImageSize.getStandardImageSizeByName(sizeName);
        return getImageByStdSize(imageHash, stdSize);
    }

    private Path getImageByStdSize(HashCode imageHash, ImageSize stdSize) {
        String partitionDir = getHashCodePartitionDir(imageHash);
        int stdwidth = stdSize.getWidth();
        int stdheight = stdSize.getHeight();
        String widthXheight = String.format("%dx%d", stdwidth, stdheight);
        String resizedImageFileName = imageHash.toString() + "_" + widthXheight
                + ".JPG";
        Path filepath = Paths.get(getImageRepoHome(), partitionDir,
                resizedImageFileName);

        if (filepath.toFile().exists()) {
            return filepath;
        }
        Path originalImagepath = Paths.get(getImageRepoHome(), partitionDir,
                imageHash + ".JPG");

        try {
            addReizedImage(originalImagepath, stdwidth, stdheight, filepath);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return filepath;
    }

    private String getHashCodePartitionDir(HashCode imageHash) {
        int partition = selectPartition(getNumberOfPartitions(), imageHash);
        String partitionDir = getPartitionPrefix() + partition;
        return partitionDir;
    }

    private static void addReizedImage(Path originalImagePath, int width,
            int height, Path filepath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(originalImagePath);
        BufferedImage image = ImageIO
                .read(new ByteArrayInputStream(imageBytes));
        Thumbnails.of(image).size(width, height).toFile(filepath.toFile());
    }

    private static HashCode toHashSHA256(byte[] bytes) {
        HashFunction sha256 = Hashing.sha256();
        HashCode hashBytes = sha256.hashBytes(bytes);
        return hashBytes;
    }

    private Map<String, String> extractMetaData(byte[] jpegBytes) {
        try {
            return jpegEXIFExtractor.extract(new ByteArrayInputStream(jpegBytes));
        } catch (Exception e) {
            throw new ImageMetadataExtractionException("Metadata extraction failed ", e);
        }
    }

    private Path addImagaMetaData(HashCode imageHash, byte[] jsonBytes) {
        int selectedPartition = selectPartition(getNumberOfPartitions(), imageHash);
        String partitionDir = getPartitionPrefix() + selectedPartition;
        Path metaFilePath = Paths.get(getImageRepoHome(), partitionDir, imageHash.toString() + ".meta");

        try {
            return Files.write(metaFilePath, jsonBytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ImageWriteException("Write failed for : " + metaFilePath.toString(), e);
        }
    }

    private static JsonObject toJson(Map<String, String> metadata) {
        JsonObject json = new JsonObject();
        metadata.entrySet().forEach(entry -> json.put(entry.getKey(), entry.getValue()));
        return json;
    }

    public static int selectPartition(int numberOfPartitions, HashCode hashcode) {
        BigInteger numberOfNodes = new BigInteger(ByteBuffer.allocate(4).putInt(numberOfPartitions).array());
        BigInteger bigInteger = new BigInteger(hashcode.asBytes());
        int mod = bigInteger.mod(numberOfNodes).intValue();
        return mod + 1;
    }


}
