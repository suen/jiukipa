package com.daubajee.jiukipa.batch;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.jpeg.JpegParser;
import org.apache.tika.sax.BodyContentHandler;

import com.google.common.collect.Maps;

public class JpegEXIFExtractor {

	public Map<String, String> extract(InputStream stream) throws Exception {
		BodyContentHandler handler = new BodyContentHandler();

		ParseContext pcontext = new ParseContext();
		JpegParser jpegParser = new JpegParser();
		Metadata metadata = new Metadata();

		jpegParser.parse(stream, handler, metadata, pcontext);
		
		String[] metadataNames = metadata.names();
		
		Map<String, String> kv = Maps.newHashMap();
		for(String name : metadataNames) {
			kv.put(name, metadata.get(name));
		}
		return kv;
	}
	
	public static void main(String[] args) throws Exception {
		String file = "/media/surendra/data/suren/Pictures/d5300/nepal_2017_trek_dashain_tihar/AnnapurnaCircuit2017_hd_res/DSC_9177.JPG";
		InputStream stream = new FileInputStream(Paths.get(file).toFile());
		new JpegEXIFExtractor().extract(stream);
	}


}
