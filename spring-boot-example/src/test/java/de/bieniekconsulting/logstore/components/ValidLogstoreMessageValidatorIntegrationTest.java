package de.bieniekconsulting.logstore.components;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import de.bieniekconsulting.logstore.TestConfiguration;
import de.bieniekconsulting.logstore.types.LogstoreMessage;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = ValidLogstoreMessageValidatorIntegrationTest.TestConfig.class)
public class ValidLogstoreMessageValidatorIntegrationTest {

	@Autowired
	private Validator validator;

	@Test
	public void shouldValidateLogstoreMessageWithClearText() {
		final LogstoreMessage message = LogstoreMessage.builder().messageText("foo").timestamp(1L).build();

		assertThat(validator.validate(message)).isEmpty();
	}

	@Test
	public void shouldValidateLogstoreMessageWithCompressedText() {
		final LogstoreMessage message = LogstoreMessage.builder().compressedEncodedMessageText("abcd").timestamp(1L)
				.build();

		assertThat(validator.validate(message)).isEmpty();
	}

	@Test
	public void shouldFailValidationOnLogstoreMessageWithoutTimestamp() {
		final LogstoreMessage message = LogstoreMessage.builder().messageText("foo").build();

		assertThat(validator.validate(message)).isNotEmpty();
	}

	@Test
	public void shouldFailValidationOnLogstoreMessageWithoutMessage() {
		final LogstoreMessage message = LogstoreMessage.builder().timestamp(1L).build();

		assertThat(validator.validate(message)).isNotEmpty();
	}

	@Test
	public void shouldFailValidationOnLogstoreMessageWithEmptyMessage() {
		final LogstoreMessage message = LogstoreMessage.builder().messageText("").timestamp(1L).build();

		assertThat(validator.validate(message)).isNotEmpty();
	}

	@Test
	public void shouldFailValidationOnLogstoreMessageWithEmptyCompressedMessage() {
		final LogstoreMessage message = LogstoreMessage.builder().compressedEncodedMessageText("").timestamp(1L)
				.build();

		assertThat(validator.validate(message)).isNotEmpty();
	}

	@Test
	public void shouldFailValidationOnLogstoreMessageWithClearAndCompressedMessage() {
		final LogstoreMessage message = LogstoreMessage.builder().compressedEncodedMessageText("abcd")
				.messageText("def").timestamp(1L).build();

		assertThat(validator.validate(message)).isNotEmpty();
	}

	@TestConfiguration
	@Import(ComponentsConfiguration.class)
	public static class TestConfig {
		@Bean
		public LocalValidatorFactoryBean localValidatorFactoryBean() {
			return new LocalValidatorFactoryBean();
		}
	}

}
