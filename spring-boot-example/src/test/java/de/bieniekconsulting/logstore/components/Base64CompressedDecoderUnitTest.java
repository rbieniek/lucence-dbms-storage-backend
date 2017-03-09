package de.bieniekconsulting.logstore.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class Base64CompressedDecoderUnitTest {

	private Base64CompressedDecoder decoder = new Base64CompressedDecoder();

	@Test
	public void shouldDecodeText() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final GZIPOutputStream gos = new GZIPOutputStream(baos);

		gos.write("foo bah".getBytes(Charset.forName("UTF-8")));
		IOUtils.closeQuietly(gos);

		final String decoded = decoder.decodeText(Base64.getEncoder().encodeToString(baos.toByteArray()));

		assertThat(decoded).isEqualTo("foo bah");
	}

	@Test(expected = RuntimeException.class)
	public void shouldFailOnMissingCompression() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		baos.write("foo bah".getBytes(Charset.forName("UTF-8")));
		IOUtils.closeQuietly(baos);

		decoder.decodeText(Base64.getEncoder().encodeToString(baos.toByteArray()));
	}

	@Test(expected = RuntimeException.class)
	public void shouldFailOnClearText() throws Exception {

		decoder.decodeText("foo bah");
	}
}
