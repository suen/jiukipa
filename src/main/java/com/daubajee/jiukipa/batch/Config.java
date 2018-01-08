package com.daubajee.jiukipa.batch;

public class Config {

    public String imageRepoHome() {
        return "/tmp/pics";
    }

    public int numberOfParition() {
        return 50;
    }

    public String partitionPrefix() {
        return "partition_";
    }

}
