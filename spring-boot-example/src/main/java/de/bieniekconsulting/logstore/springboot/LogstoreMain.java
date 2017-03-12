package de.bieniekconsulting.logstore.springboot;

import org.springframework.boot.SpringApplication;

public class LogstoreMain {

	public static void main(final String[] args) {
		new SpringApplication(LogstoreConfiguration.class).run(args);
	}

}
