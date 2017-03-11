package de.bieniekconsulting.logstore.camel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import de.bieniekconsulting.jdbc.logstore.LogstoreRecord;
import de.bieniekconsulting.jdbc.logstore.LogstoreService;
import de.bieniekconsulting.logstore.LogstoreConfiguration;
import de.bieniekconsulting.logstore.components.ComponentsConfiguration;
import de.bieniekconsulting.logstore.types.LogstoreMessage;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceService;
import de.bieniekconsulting.springframework.support.TestConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = LogstoreRouteBuilderIntegrationTest.TestConfig.class)
public class LogstoreRouteBuilderIntegrationTest {

	@LocalServerPort
	private int localServerPort;

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	@Test
	public void shouldRejectHttpMethodPutOnStore() {
		final RestTemplate template = restTemplateBuilder.build();

		try {
			template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.PUT,
					new HttpEntity<>(""), Void.class);

			fail();
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		}

	}

	@Test
	public void shouldRejectHttpMethodPutOnRetrieve() {
		final RestTemplate template = restTemplateBuilder.build();

		try {
			template.exchange("http://localhost:" + localServerPort + "/camel/log/retrieve", HttpMethod.PUT,
					new HttpEntity<>(""), Void.class);

			fail();
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		}

	}

	@Test
	public void shouldRejectHttpMethodDeleteOnStore() {
		final RestTemplate template = restTemplateBuilder.build();

		try {
			template.delete("http://localhost:" + localServerPort + "/camel/log/store");

			fail();
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		}

	}

	@Test
	public void shouldRejectHttpMethodDeleteOnRetrieve() {
		final RestTemplate template = restTemplateBuilder.build();

		try {
			template.delete("http://localhost:" + localServerPort + "/camel/log/retrieve");

			fail();
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		}

	}

	@Test
	public void shouldRejectHttpMethodGetOnStore() {
		final RestTemplate template = restTemplateBuilder.build();

		try {
			template.getForEntity("http://localhost:" + localServerPort + "/camel/log/store", LogstoreMessage.class);

			fail();
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		}

	}

	@Test
	public void shouldPassHttpMethodPostOnStoreWithClearText() {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
				new HttpEntity<>(
						LogstoreMessage.builder().timestamp(System.currentTimeMillis()).messageText("foo").build()),
				Void.class);
	}

	@Test
	public void shouldPassHttpMethodPostOnStoreWithCompressedText() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final GZIPOutputStream gos = new GZIPOutputStream(baos);

		gos.write("foo bah".getBytes(Charset.forName("UTF-8")));

		IOUtils.closeQuietly(gos);

		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
				new HttpEntity<>(LogstoreMessage.builder().timestamp(System.currentTimeMillis())
						.compressedEncodedMessageText(Base64.getEncoder().encodeToString(baos.toByteArray())).build()),
				Void.class);
	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithMissingTimestamp() {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		try {
			template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
					new HttpEntity<>(LogstoreMessage.builder().messageText("foo").build()), Void.class);
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		}
	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithCorruptedCompressedText() {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		try {
			template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
					new HttpEntity<>(
							LogstoreMessage.builder().timestamp(1L).compressedEncodedMessageText("foo").build()),
					Void.class);
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		}
	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithNonJsonPayload() {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		try {
			template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
					new HttpEntity<>("foo"), Void.class);
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		}
	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithEmptyPayload() {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		try {
			template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
					new HttpEntity<>(""), Void.class);
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		}
	}

	@Test
	public void shouldRejectHttpMethodPostOnRetrieve() {
		final RestTemplate template = restTemplateBuilder.build();

		try {
			template.exchange("http://localhost:" + localServerPort + "/camel/log/retrieve", HttpMethod.POST,
					new HttpEntity<>(""), Void.class);

			fail();
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		}

	}

	@Test
	public void shouldAccessApiDocs() throws Exception {
		final RestTemplate template = restTemplateBuilder.build();

		template.getForEntity("http://localhost:" + localServerPort + "/camel/api-doc", Object.class);
	}

	@Test
	public void shouldRetrievingStoredLogMessage() throws Exception {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		final ResponseEntity<LogstoreMessage> responseEntity = template
				.getForEntity("http://localhost:" + localServerPort + "/camel/log/retrieve/1", LogstoreMessage.class);
		final LogstoreMessage message = responseEntity.getBody();

		assertThat(message).isNotNull();
		assertThat(message.getMessageText()).isEqualTo("foo bar");
		assertThat(message.getTimestamp()).isEqualTo(1L);
	}

	@Test
	public void shouldNotRetrievingUnknownLogMessage() throws Exception {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		try {
			template.getForEntity("http://localhost:" + localServerPort + "/camel/log/retrieve/2",
					LogstoreMessage.class);

			fail();
		} catch (final HttpClientErrorException e) {
			assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void shouldSearchAndFindExistingLog() throws Exception {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		final ResponseEntity<List> responseEntity = template
				.getForEntity("http://localhost:" + localServerPort + "/camel/log/search?term=foo", List.class);
		final List<Link> links = responseEntity.getBody();

		assertThat(links).hasSize(1).isNotNull();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void shouldSearchAndNotFindNotExistingLog() throws Exception {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		final ResponseEntity<List> responseEntity = template
				.getForEntity("http://localhost:" + localServerPort + "/camel/log/search?term=bar", List.class);
		final List<Link> links = responseEntity.getBody();

		assertThat(links).hasSize(0).isNotNull();
	}

	@TestConfiguration
	@Import({ CamelConfiguration.class, LogstoreConfiguration.FormLoginSecurityConfig.class,
			ComponentsConfiguration.class })
	@EnableAutoConfiguration
	public static class TestConfig {
		@Bean
		public LogstoreService logstoreService() {
			final LogstoreService service = mock(LogstoreService.class);

			when(service.persistLogstoreRecord(any(LogstoreRecord.class))).thenAnswer(args -> {
				final LogstoreRecord message = args.getArgumentAt(0, LogstoreRecord.class);

				return LogstoreRecord.builder().messageText(message.getMessageText()).timestamp(message.getTimestamp())
						.id(1L).build();
			});

			when(service.retrieveLogstoreRecord(any(Long.class))).then(args -> {
				final Long id = args.getArgumentAt(0, Long.class);

				if (id == 1) {
					return Optional.of(LogstoreRecord.builder().id(id).messageText("foo bar").timestamp(1L).build());
				} else {
					return Optional.empty();
				}
			});

			return service;
		}

		@SuppressWarnings("unchecked")
		@Bean
		public LucenceService luceneService() throws Exception {
			final LucenceService service = mock(LucenceService.class);

			when(service.searchLogRecords(any(Set.class), any(Integer.class))).then(args -> {
				final Set<String> terms = args.getArgumentAt(0, Set.class);
				final Set<Long> ids = new HashSet<>();

				if (terms.contains("foo")) {
					ids.add(1L);
				}

				return ids;
			});

			return service;
		}
	}
}
