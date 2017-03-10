package de.bieniekconsulting.jdbc.logstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

import java.sql.PreparedStatement;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.springframework.support.TestConfiguration;
import liquibase.integration.spring.SpringLiquibase;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LogstoreServiceIntegrationTest.TestConfig.class)
public class LogstoreServiceIntegrationTest {

	@Autowired
	private LogstoreService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void shouldPersistLogstoreRecord() {
		final LogstoreRecord record = service
				.persistLogstoreRecord(LogstoreRecord.builder().messageText("foo bar").timestamp(1L).build());

		assertThat(record).isNotNull();
		assertThat(record.getId()).isGreaterThan(0L);
		assertThat(record.getMessageText()).isEqualTo("foo bar");
		assertThat(record.getTimestamp()).isEqualTo(1L);

		assertThat(jdbcTemplate.query((PreparedStatementCreator) con -> {
			final PreparedStatement ps = con
					.prepareStatement("select id, timestamp, message_text from LOGSTORE_RECORDS where ID=?");

			ps.setLong(1, record.getId());

			return ps;
		}, (ResultSetExtractor<Boolean>) rs -> rs.next())).isTrue();
	}

	@Test
	public void shouldRetrieveLogstoreRecord() {
		jdbcTemplate.update(con -> {
			final PreparedStatement ps = con
					.prepareStatement("insert into LOGSTORE_RECORDS (id, timestamp, message_text) values (?,?,?)");

			ps.setLong(1, 2048);
			ps.setLong(2, 10240);
			ps.setString(3, "bar bar foo");

			return ps;
		});

		final Optional<LogstoreRecord> record = service.retrieveLogstoreRecord(2048);

		assertThat(record).isNotEmpty().hasValueSatisfying(rec -> assertThat(rec.getId()).isEqualTo(2048))
				.hasValueSatisfying(rec -> assertThat(rec.getTimestamp()).isEqualTo(10240))
				.hasValueSatisfying(rec -> assertThat(rec.getMessageText()).isEqualTo("bar bar foo"));
	}

	@TestConfiguration
	@Import(JdbcLogstoreConfiguration.class)
	public static class TestConfig {

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
			springLiquibase.setChangeLog("classpath:db/changelog/db.logstore-changelog.xml");

			return springLiquibase;
		}

		@Bean
		@Autowired
		public JdbcTemplate jdbcTemplate(final DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}
	}
}
