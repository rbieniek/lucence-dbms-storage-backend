package de.bieniekconsulting.logstore.wildfly.spring;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.jta.JtaTransactionManager;

import de.bieniekconsulting.jdbc.logstore.JdbcLogstoreConfiguration;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceConfiguration;
import liquibase.integration.spring.SpringLiquibase;

@Configuration
@Import({ LucenceConfiguration.class, JdbcLogstoreConfiguration.class })
public class WildflySpringConfiguration {
	@Bean
	public JtaTransactionManager jtaTransactionManager() {
		return new JtaTransactionManager();
	}

	@Bean
	public DataSource dataSource() {
		final JndiDataSourceLookup lookup = new JndiDataSourceLookup();

		return lookup.getDataSource("jdbc/lucene");
	}

	@Bean
	@Autowired
	public JdbcTemplate jdbcTemplate(final DataSource dataSource) {
		final JdbcTemplate template = new JdbcTemplate();

		template.setDataSource(dataSource);

		return template;
	}

	@Bean
	@Autowired
	public SpringLiquibase springLiquibase(final DataSource dataSource) {
		final SpringLiquibase springLiquibase = new SpringLiquibase();

		springLiquibase.setDataSource(dataSource);
		springLiquibase.setChangeLog("classpath:db/changelog/db.changelog.xml");

		return springLiquibase;
	}

}
