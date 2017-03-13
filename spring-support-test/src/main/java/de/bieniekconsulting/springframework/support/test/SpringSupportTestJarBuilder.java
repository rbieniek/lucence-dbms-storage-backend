package de.bieniekconsulting.springframework.support.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import de.bieniekconsulting.springframework.support.TestConfiguration;

public class SpringSupportTestJarBuilder {
	public static JavaArchive jar() {
		return ShrinkWrap.create(JavaArchive.class).addClass(TestConfiguration.class);
	}
}
