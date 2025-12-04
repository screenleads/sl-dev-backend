package com.screenleads.backend.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AppApplicationTests {

	@Test
	void contextLoads() {
		// This test simply verifies that the Spring application context loads
		// successfully
	}

}
