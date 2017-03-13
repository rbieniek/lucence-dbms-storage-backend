package de.bieniekconsulting.logstore.wildfly.spring;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import de.bieniekconsulting.springcdi.bridge.api.ApplicationContextProvider;

public class WildflyApplicationContextProvider implements ApplicationContextProvider {
	@Override
	public ConfigurableApplicationContext provideContext() {
		return new AnnotationConfigApplicationContext(WildflySpringConfiguration.class);
	}

}
