package com.daubajee.jiukipa.batch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ImageSorter {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("s","src", true, "source directory to scan");
		options.addOption("d","dest", true, "Destination Directory");
		options.addOption("p","partition-prefix", true, "Partition Prefix");
		options.addOption("n","nb-partition", true, "Number of partitions");
		
		CommandLineParser parser = new DefaultParser();
		try {
			
			CommandLine cmd = parser.parse(options, args);
			
			if (!cmd.hasOption("src") 
					|| !cmd.hasOption("dest") 
					|| !cmd.hasOption("partition-prefix") 
					|| !cmd.hasOption("nb-partition")) {
				new HelpFormatter().printHelp("imagesorter", options);
			}
		
			
			
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}
	
}

