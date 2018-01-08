package com.daubajee.jiukipa.batch;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.daubajee.jiukipa.image.JpegEXIFExtractor;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.vertx.core.json.JsonObject;
import net.coobird.thumbnailator.Thumbnails;


public class Hasher {

	 
	
	public HashCode getSha256(byte[] bytes) {
		HashFunction sha256 = Hashing.sha256();
		HashCode hashBytes = sha256.hashBytes(bytes);
		return hashBytes;
	}
	
	public static String[] makeNodeDirectories(String parentDir, int n) {
		String[] names = new String[n];
		for (int i = 0; i < n; i++) {
			String name = "dir"+i;
			File file = Paths.get(parentDir, name).toFile();
			file.mkdir();
			if (!file.isDirectory()) {
				System.out.println(name + " not created");
			}
			names[i] = name;
		}
		return names;
	}
	
	public static void main(String[] args) throws Exception {
		
		String file = "/media/surendra/data/suren/Pictures/d5300/nepal_2017_trek_dashain_tihar/AnnapurnaCircuit2017_hd_res";
		String target = "/tmp/pics";
		int numberOfNodes = 50;
		String[] nodeNames = makeNodeDirectories(target, numberOfNodes);
		BigInteger modulusFactor = new BigInteger(String.valueOf(numberOfNodes));
		JpegEXIFExtractor jpegEXIFExtractor = new JpegEXIFExtractor();
		File dir = new File(file);
		File[] files = dir.listFiles();
		Arrays.asList(files).stream()
			.parallel()
			.forEach(item -> {
			if (item.isDirectory()) {
				return;
			}
			try {
				byte[] bytes = Files.readAllBytes(item.toPath());
				
				Map<String, String> metadata = jpegEXIFExtractor.extract(new ByteArrayInputStream(bytes));
				String width = metadata.get("tiff:ImageWidth");
				String height = metadata.get("tiff:ImageLength");

				HashCode hashcode = new Hasher().getSha256(bytes);
				
				
				int modulus = selectNode(modulusFactor, hashcode);
				String node = nodeNames[modulus];
				
				byte[] jsonBytes = getJsonBytes(metadata);
				
				Path filepath = createTargetFilePath(target, hashcode, node, width, height);
				Path metafilepath = createTargetMetaFilePath(target, hashcode, node);
				
				Files.write(filepath, bytes, StandardOpenOption.CREATE_NEW);
				Files.write(metafilepath, jsonBytes, StandardOpenOption.CREATE_NEW);
				
				List<ImageSize> profiles = calculateResizeProfile(Integer.parseInt(width), Integer.parseInt(height));
				for (ImageSize profile : profiles) {
					Path resizedfilepath = createTargetFilePath(target, hashcode, node, profile.getWidth(), profile.getHeight());
					writeReizedImage(bytes, profile.getWidth(), profile.getHeight(), resizedfilepath);
				}
				
			} catch (Exception e) {
				System.err.println(item.getAbsolutePath());
				e.printStackTrace();
			}
		});
	}
	
	private static List<ImageSize> calculateResizeProfile(int width, int height) {
		return ImageSize.getProfiles(width, height)
			.stream()
			.map(profile -> {
				if (profile.isLandscape()) {
					float ratio = (float) width / (float) height;
					return new ImageSize(profile.getWidth(), (int) ((float)profile.getWidth() / ratio));
				}
				float ratio = (float) height / (float) width;
				return new ImageSize((int) ((float) profile.getWidth() / ratio), profile.getHeight());
			})
			.collect(Collectors.toList());
	}

	private static void writeReizedImage(byte[] imageBytes, int width, int height, Path filepath) throws IOException {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
		Thumbnails.of(image).size(width, height).toFile(filepath.toFile());
	}

	private static Path createTargetMetaFilePath(String target, HashCode hashcode, String node) {
		return Paths.get(target, node, hashcode.toString() + ".meta");
	}

	private static byte[] getJsonBytes(Map<String, String> metadata) {
		JsonObject json = new JsonObject();
		metadata.entrySet().forEach(entry -> json.put(entry.getKey(), entry.getValue()));
		return json.toString().getBytes();
	}
	
	private static Path createTargetFilePath(String target, HashCode hashcode, String node, Integer width, Integer height) {
		String filename = String.format("%s_%dx%d.JPG", hashcode.toString(), width, height);
		return Paths.get(target, node, filename);
	}

	private static Path createTargetFilePath(String target, HashCode hashcode, String node, String width, String height) {
		String filename = String.format("%s_%sx%s.JPG", hashcode.toString(), width, height);
		return Paths.get(target, node, filename);
	}

	private static int selectNode(BigInteger m, HashCode hashcode) {
		BigInteger bigInteger = new BigInteger(hashcode.asBytes());
		int mod = bigInteger.mod(m).intValue();
		return mod;
	}
	
}
