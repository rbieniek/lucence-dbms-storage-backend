package de.bieniekconsulting.logstore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.logstore.TestConfiguration;
import de.bieniekconsulting.logstore.types.LogstoreMessage;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = LogstoreServiceIntegrationTest.TestConfig.class)
public class LogstoreServiceIntegrationTest {

	@Autowired
	private LogstoreService service;

	@Autowired
	private LogstoreRepository repository;

	@Test
	public void shouldPersistLogMessage() {
		final LogstoreRecord record = service
				.persistLogstoreMessage(LogstoreMessage.builder().messageText("foo bar").timestamp(1L).build());

		assertThat(record).isNotNull();
		assertThat(record.getId()).isGreaterThan(0L);
		assertThat(record.getMessageText()).isEqualTo("foo bar");
		assertThat(record.getTimestamp()).isEqualTo(1L);

		assertThat(repository.findOne(record.getId())).isNotNull();
	}

	@TestConfiguration
	@EnableAutoConfiguration
	@Import(PersistenceConfiguration.class)
	public static class TestConfig {

	}
}
