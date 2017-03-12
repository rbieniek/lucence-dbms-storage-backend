package de.bieniekconsulting.logstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.annotation.DirtiesContext.HierarchyMode.EXHAUSTIVE;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import de.bieniekconsulting.jdbc.logstore.JdbcLogstoreConfiguration;
import de.bieniekconsulting.logstore.camel.CamelConfiguration;
import de.bieniekconsulting.logstore.components.ComponentsConfiguration;
import de.bieniekconsulting.logstore.types.LogstoreMessage;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceConfiguration;
import de.bieniekconsulting.springframework.support.TestConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = LogstoreIntegrationTest.TestConfig.class)
@DirtiesContext(classMode = AFTER_CLASS, hierarchyMode = EXHAUSTIVE)
public class LogstoreIntegrationTest {
	private final char[] alphas = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
			'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	@LocalServerPort
	private int localServerPort;

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

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
	public void shouldAccessApiDocs() throws Exception {
		final RestTemplate template = restTemplateBuilder.build();

		template.getForEntity("http://localhost:" + localServerPort + "/camel/api-doc", Object.class);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void shouldSearchAndNotFindNotExistingLog() throws Exception {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
				new HttpEntity<>(
						LogstoreMessage.builder().timestamp(System.currentTimeMillis()).messageText("foo").build()),
				Void.class);

		final ResponseEntity<List> responseEntity = template
				.getForEntity("http://localhost:" + localServerPort + "/camel/log/search?term=bar", List.class);
		final List<Link> links = responseEntity.getBody();

		assertThat(links).hasSize(0).isNotNull();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void shouldFindOneWord() throws Exception {
		final RestTemplate template = restTemplateBuilder
				.additionalMessageConverters(new MappingJackson2HttpMessageConverter()).build();

		template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
				new HttpEntity<>(
						LogstoreMessage.builder().timestamp(System.currentTimeMillis()).messageText("foo").build()),
				Void.class);

		for (int l1 = 0; l1 < 26; l1++) {
			for (int l2 = 0; l2 < 26; l2++) {
				final StringBuffer buffer = new StringBuffer();

				for (int l3 = 0; l3 < 26; l3++) {
					buffer.append('a');
					buffer.append(alphas[l1]);
					buffer.append(alphas[l2]);
					buffer.append(alphas[l3]);
					buffer.append(' ');
				}

				final String line = buffer.toString();

				template.exchange("http://localhost:" + localServerPort + "/camel/log/store", HttpMethod.POST,
						new HttpEntity<>(LogstoreMessage.builder().timestamp(System.currentTimeMillis())
								.messageText(line).build()),
						Void.class);
			}
		}

		final ResponseEntity<List> searchResponseEntity = template
				.getForEntity("http://localhost:" + localServerPort + "/camel/log/search?term=accc", List.class);
		final List<Map<String, String>> links = searchResponseEntity.getBody();

		assertThat(links).hasSize(1).isNotNull();

		final ResponseEntity<LogstoreMessage> logResponseEntity = template.getForEntity(links.get(0).get("href"),
				LogstoreMessage.class);
		final LogstoreMessage message = logResponseEntity.getBody();

		assertThat(message).isNotNull();
		assertThat(message.getMessageText()).contains("accc");
		assertThat(message.getTimestamp()).isNotNull();

	}

	@TestConfiguration
	@EnableAutoConfiguration
	@Import({ CamelConfiguration.class, LogstoreConfiguration.FormLoginSecurityConfig.class,
			ComponentsConfiguration.class, LucenceConfiguration.class, JdbcLogstoreConfiguration.class })
	public static class TestConfig {

	}
}
