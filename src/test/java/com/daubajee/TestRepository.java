package com.daubajee;

import org.junit.Test;

import com.daubajee.jiukipa.model.Repository;

public class TestRepository {

	@Test
	public void testInit() {
		Repository repository = new Repository();
		repository.init("/tmp/pics");
	}
	
}
