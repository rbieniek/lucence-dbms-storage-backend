package de.bieniekconsulting.logstore.wildfly.camel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Link;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import de.bieniekconsulting.lucene.jdbc.directory.LucenceService;

public class SearchLogMessageEndpoint {
	@Inject
	private LucenceService luceneService;

	public void searchLogMessages(final Exchange exchange) throws IOException, URISyntaxException {
		final HttpServletRequest request = exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST,
				HttpServletRequest.class);

		final URI baseUri = new URI(request.getScheme(), null, request.getServerName(), request.getServerPort(),
				request.getServletPath(), null, null);

		final String term = exchange.getIn().getHeader("term", String.class);
		final Integer limit = exchange.getIn().getHeader("limit", 100, Integer.class);

		final List<Link> links = new LinkedList<>();

		if (StringUtils.isNotBlank(term)) {
			final Set<String> terms = new HashSet<>();

			terms.add(term);
			luceneService.searchLogRecords(terms, limit).forEach(id -> {
				links.add(Link.fromUri("/log/retrieve/{id}").baseUri(baseUri).rel("self").type("application/json")
						.build(id));
			});
		}

		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_OK);
		exchange.getOut().setBody(links);
	}
}
