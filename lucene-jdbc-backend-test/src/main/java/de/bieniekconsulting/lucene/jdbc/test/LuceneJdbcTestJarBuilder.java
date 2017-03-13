package de.bieniekconsulting.lucene.jdbc.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import de.bieniekconsulting.springframework.support.TestConfiguration;

public class LuceneJdbcTestJarBuilder {
	public static JavaArchive extensionJar() {
		return ShrinkWrap.create(JavaArchive.class).addClass(TestConfiguration.class);
	}
}
