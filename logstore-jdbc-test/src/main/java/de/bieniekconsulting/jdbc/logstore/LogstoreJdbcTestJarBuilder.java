package de.bieniekconsulting.jdbc.logstore;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class LogstoreJdbcTestJarBuilder {
	public static JavaArchive extensionJar() {
		return ShrinkWrap.create(JavaArchive.class).addClass(JdbcLogstoreConfiguration.class)
				.addClass(LogstoreRecord.class).addClass(LogstoreService.class);
	}
}
