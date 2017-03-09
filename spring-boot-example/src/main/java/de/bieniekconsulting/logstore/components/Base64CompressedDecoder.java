package de.bieniekconsulting.logstore.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

@Component
public class Base64CompressedDecoder {

	public String decodeText(final String base64CompText) {
		try {
			final ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64CompText));
			final GZIPInputStream gis = new GZIPInputStream(bais);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();

			IOUtils.copy(gis, baos);
			IOUtils.closeQuietly(gis);
			IOUtils.closeQuietly(baos);

			return new String(baos.toByteArray(), Charset.forName("UTF-8"));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
