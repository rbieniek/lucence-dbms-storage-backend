package de.bieniekconsulting.jdbc.logstore;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class LogstoreJdbcTestJarBuilder {
	public static JavaArchive jar() {
		return ShrinkWrap.create(JavaArchive.class).addClass(JdbcLogstoreConfiguration.class)
				.addClass(LogstoreRecord.class).addClass(LogstoreService.class)
				.addAsResource("db/changelog/db.logstore-changelog.xml");
	}
}
