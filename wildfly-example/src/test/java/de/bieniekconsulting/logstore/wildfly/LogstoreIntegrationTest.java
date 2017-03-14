package de.bieniekconsulting.logstore.wildfly;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.bieniekconsulting.logstore.wildfly.test.WildflyTestArchivesBuilder;
import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@RunWith(Arquillian.class)
public class LogstoreIntegrationTest {
	private final char[] alphas = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
			'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	public static final MediaType JSON = MediaType.parse("application/json");

	@ArquillianResource
	private URL deploymentURL;

	@Deployment(testable = false)
	public static final WebArchive deployment() {
		return WildflyTestArchivesBuilder.war();
	}

	private OkHttpClient httpClient;

	@Before
	public void before() {
		httpClient = new OkHttpClient();
	}

	@Test
	public void shouldSearchAndNotFindNotExistingLog() throws Exception {
		final RequestBody body = RequestBody.create(JSON, encodeLogstoreMessage(
				LogstoreMessage.builder().messageText("foo").timestamp(System.currentTimeMillis()).build()));

		final Request storeRequest = new Request.Builder().url(url("/camel/log/store")).post(body).build();
		final Response storeResponse = httpClient.newCall(storeRequest).execute();

		assertThat(storeResponse.code(), is(HttpStatus.SC_CREATED));

		final Request searchRequest = new Request.Builder().url(url("/camel/log/search?term=bar")).get().build();
		final Response searchResponse = httpClient.newCall(searchRequest).execute();

		assertThat(searchResponse.code(), is(HttpStatus.SC_OK));

		final List<Map<String, Object>> links = readResponse(searchResponse.body().byteStream());

		assertThat(links.size(), is(0));
	}

	@Test
	public void shouldFindOneWord() throws Exception {

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

				final RequestBody body = RequestBody.create(JSON, encodeLogstoreMessage(
						LogstoreMessage.builder().messageText(line).timestamp(System.currentTimeMillis()).build()));

				final Request storeRequest = new Request.Builder().url(url("/camel/log/store")).post(body).build();
				final Response storeResponse = httpClient.newCall(storeRequest).execute();

				assertThat(storeResponse.code(), is(HttpStatus.SC_CREATED));
			}
		}

		final Request searchRequest = new Request.Builder().url(url("/camel/log/search?term=accc")).get().build();
		final Response searchResponse = httpClient.newCall(searchRequest).execute();

		assertThat(searchResponse.code(), is(HttpStatus.SC_OK));

		final List<Map<String, Object>> links = readResponse(searchResponse.body().byteStream());

		assertThat(links.size(), is(1));

		final Map<String, Object> link = links.get(0);

		final Request retrieveRequest = new Request.Builder().url(url("/camel" + link.get("uri"))).get().build();
		final Response retrieveResponse = httpClient.newCall(retrieveRequest).execute();

		assertThat(retrieveResponse.code(), is(HttpStatus.SC_OK));

		final LogstoreMessage logEntry = readLogstoreMessage(retrieveResponse.body().byteStream());

		assertThat(logEntry.getMessageText(), Matchers.containsString("accc"));
	}

	@Test
	public void shouldRejectHttpMethodPutOnStore() throws Exception {
		final RequestBody body = RequestBody.create(JSON, new byte[0]);

		final Request request = new Request.Builder().url(url("/camel/log/store")).put(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_METHOD_NOT_ALLOWED));

	}

	@Test
	public void shouldRejectHttpMethodPutOnRetrieve() throws Exception {
		final RequestBody body = RequestBody.create(JSON, new byte[0]);

		final Request request = new Request.Builder().url(url("/camel/log/retrieve")).put(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_METHOD_NOT_ALLOWED));
	}

	@Test
	public void shouldRejectHttpMethodDeleteOnStore() throws Exception {
		final Request request = new Request.Builder().url(url("/camel/log/store")).delete().build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_METHOD_NOT_ALLOWED));
	}

	@Test
	public void shouldRejectHttpMethodDeleteOnRetrieve() throws Exception {
		final Request request = new Request.Builder().url(url("/camel/log/retrieve")).delete().build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_METHOD_NOT_ALLOWED));
	}

	@Test
	public void shouldRejectHttpMethodGetOnStore() throws Exception {
		final Request request = new Request.Builder().url(url("/camel/log/store")).get().build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_METHOD_NOT_ALLOWED));
	}

	@Test
	public void shouldPassHttpMethodPostOnStoreWithClearText() throws Exception {
		final RequestBody body = RequestBody.create(JSON, encodeLogstoreMessage(
				LogstoreMessage.builder().messageText("foo bar").timestamp(System.currentTimeMillis()).build()));

		final Request request = new Request.Builder().url(url("/camel/log/store")).post(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_CREATED));
	}

	@Test
	public void shouldPassHttpMethodPostOnStoreWithCompressedText() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final GZIPOutputStream gos = new GZIPOutputStream(baos);

		gos.write("foo bah".getBytes(Charset.forName("UTF-8")));

		IOUtils.closeQuietly(gos);

		final RequestBody body = RequestBody.create(JSON,
				encodeLogstoreMessage(LogstoreMessage.builder()
						.compressedEncodedMessageText(Base64.getEncoder().encodeToString(baos.toByteArray()))
						.timestamp(System.currentTimeMillis()).build()));

		final Request request = new Request.Builder().url(url("/camel/log/store")).post(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_CREATED));
	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithMissingTimestamp() throws Exception {
		final RequestBody body = RequestBody.create(JSON,
				encodeLogstoreMessage(LogstoreMessage.builder().messageText("foo bar").build()));

		final Request request = new Request.Builder().url(url("/camel/log/store")).post(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_BAD_REQUEST));
	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithCorruptedCompressedText() throws Exception {
		final RequestBody body = RequestBody.create(JSON, encodeLogstoreMessage(LogstoreMessage.builder()
				.compressedEncodedMessageText("foo bar").timestamp(System.currentTimeMillis()).build()));

		final Request request = new Request.Builder().url(url("/camel/log/store")).post(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_BAD_REQUEST));
	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithNonJsonPayload() throws Exception {
		final RequestBody body = RequestBody.create(JSON, "foo bar");

		final Request request = new Request.Builder().url(url("/camel/log/store")).post(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_BAD_REQUEST));

	}

	@Test
	public void shouldRejectHttpMethodPostOnStoreWithEmptyPayload() throws Exception {
		final RequestBody body = RequestBody.create(JSON, "");

		final Request request = new Request.Builder().url(url("/camel/log/store")).post(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_BAD_REQUEST));
	}

	@Test
	public void shouldRejectHttpMethodPostOnRetrieve() throws Exception {
		final RequestBody body = RequestBody.create(JSON, new byte[0]);

		final Request request = new Request.Builder().url(url("/camel/log/retrieve")).post(body).build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_METHOD_NOT_ALLOWED));
	}

	@Test
	public void shouldAccessApiDocs() throws Exception {
		final Request request = new Request.Builder().url(url("/camel/api-doc")).get().build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_OK));
	}

	/*
	 * @Test public void shouldRetrievingStoredLogMessage() throws Exception {
	 * final RestTemplate template = restTemplateBuilder
	 * .additionalMessageConverters(new
	 * MappingJackson2HttpMessageConverter()).build();
	 *
	 * final ResponseEntity<LogstoreMessage> responseEntity = template
	 * .getForEntity("http://localhost:" + localServerPort +
	 * "/camel/log/retrieve/1", LogstoreMessage.class); final LogstoreMessage
	 * message = responseEntity.getBody();
	 *
	 * assertThat(message).isNotNull();
	 * assertThat(message.getMessageText()).isEqualTo("foo bar");
	 * assertThat(message.getTimestamp()).isEqualTo(1L); }
	 */

	@Test
	public void shouldNotRetrievingUnknownLogMessage() throws Exception {
		final Request request = new Request.Builder().url(url("/camel/log/retrieve/1234567890")).get().build();
		final Response response = httpClient.newCall(request).execute();

		assertThat(response.code(), is(HttpStatus.SC_NOT_FOUND));
	}

	/*
	 * @Test
	 *
	 * @SuppressWarnings({ "unchecked", "rawtypes" }) public void
	 * shouldSearchAndFindExistingLog() throws Exception { final RestTemplate
	 * template = restTemplateBuilder .additionalMessageConverters(new
	 * MappingJackson2HttpMessageConverter()).build();
	 *
	 * final ResponseEntity<List> responseEntity = template
	 * .getForEntity("http://localhost:" + localServerPort +
	 * "/camel/log/search?term=foo", List.class); final List<Link> links =
	 * responseEntity.getBody();
	 *
	 * assertThat(links).hasSize(1).isNotNull(); }
	 *
	 * @Test
	 *
	 * @SuppressWarnings({ "unchecked", "rawtypes" }) public void
	 * shouldSearchAndNotFindNotExistingLog() throws Exception { final
	 * RestTemplate template = restTemplateBuilder
	 * .additionalMessageConverters(new
	 * MappingJackson2HttpMessageConverter()).build();
	 *
	 * final ResponseEntity<List> responseEntity = template
	 * .getForEntity("http://localhost:" + localServerPort +
	 * "/camel/log/search?term=bar", List.class); final List<Link> links =
	 * responseEntity.getBody();
	 *
	 * assertThat(links).hasSize(0).isNotNull(); }
	 */

	private byte[] encodeLogstoreMessage(final LogstoreMessage message) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		mapper.writeValue(baos, message);

		baos.close();

		return baos.toByteArray();
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> readResponse(final InputStream stream) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();

		return mapper.readValue(stream, List.class);
	}

	private LogstoreMessage readLogstoreMessage(final InputStream stream) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();

		return mapper.readValue(stream, LogstoreMessage.class);
	}

	private final URL url(final String path) throws Exception {
		final String realPath;

		if (path.startsWith("/")) {
			realPath = deploymentURL.getPath() + path.substring(1);
		} else {
			realPath = deploymentURL.getPath() + path;
		}

		final URL url = new URL(deploymentURL.getProtocol(), deploymentURL.getHost(), deploymentURL.getPort(),
				realPath);

		return url;
	}

}
