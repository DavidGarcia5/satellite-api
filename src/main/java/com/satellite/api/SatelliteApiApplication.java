package com.satellite.api;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SecurityScheme(
	name = "basicAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "basic",
	in = SecuritySchemeIn.HEADER
)
public class SatelliteApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SatelliteApiApplication.class, args);
	}

}
