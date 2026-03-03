package com.coinguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootTest
@ActiveProfiles("test")
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
class CoinguardBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
