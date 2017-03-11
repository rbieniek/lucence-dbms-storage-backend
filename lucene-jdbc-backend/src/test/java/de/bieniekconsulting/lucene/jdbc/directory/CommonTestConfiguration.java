package de.bieniekconsulting.lucene.jdbc.directory;

import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import de.bieniekconsulting.springframework.support.TestConfiguration;
import liquibase.integration.spring.SpringLiquibase;

@TestConfiguration
public class CommonTestConfiguration {

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(H2).setScriptEncoding("UTF-8")
				.ignoreFailedDrops(true).build();
	}

	@Bean
	@Autowired
	public SpringLiquibase springLiquibase(final DataSource dataSource) {
		final SpringLiquibase springLiquibase = new SpringLiquibase();

		springLiquibase.setDataSource(dataSource);
		springLiquibase.setChangeLog("classpath:db/changelog/db.lucene-changelog.xml");

		return springLiquibase;
	}
}
