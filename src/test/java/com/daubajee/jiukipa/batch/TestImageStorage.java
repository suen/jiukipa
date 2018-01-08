package com.daubajee.jiukipa.batch;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.daubajee.jiukipa.image.ImageStorage;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import io.vertx.core.Vertx;

public class TestImageStorage {

    @Test
    public void test() {
        ImageStorage imageStorage = new ImageStorage(new Config(),
                Vertx.vertx());
        
        Set<Integer> selectedNodes = new HashSet<>();

        while (selectedNodes.size() != 50) {
            byte[] randomBytes = UUID.randomUUID().toString().getBytes();
            HashCode testBytes = Hashing.sha256().hashBytes(randomBytes);
            int selectedNode = imageStorage.selectPartition(50, testBytes);
            Assert.assertThat((selectedNode <= 50 && selectedNode > 0), org.hamcrest.CoreMatchers.is(true));
            selectedNodes.add(selectedNode);
        }

    }

}
