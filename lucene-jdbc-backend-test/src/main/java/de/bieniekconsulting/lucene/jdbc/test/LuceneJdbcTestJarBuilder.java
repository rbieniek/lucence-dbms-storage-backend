package de.bieniekconsulting.lucene.jdbc.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import de.bieniekconsulting.lucene.jdbc.directory.JdbcDirectory;
import de.bieniekconsulting.lucene.jdbc.types.LogRecord;

public class LuceneJdbcTestJarBuilder {
	public static JavaArchive jar() {
		return ShrinkWrap.create(JavaArchive.class).addPackage(JdbcDirectory.class.getPackage())
				.addPackage(LogRecord.class.getPackage()).addAsResource("db/changelog/db.lucene-changelog.xml");
	}
}
