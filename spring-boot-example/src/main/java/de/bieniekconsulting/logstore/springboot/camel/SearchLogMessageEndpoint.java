package de.bieniekconsulting.logstore.springboot.camel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.lucene.jdbc.directory.LucenceService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SearchLogMessageEndpoint {
	private final LucenceService luceneService;

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
				links.add(new Link(baseUri.toString() + "/log/retrieve/" + id));
			});
		}

		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.OK.value());
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.OK.getReasonPhrase());
		exchange.getOut().setBody(links);
	}
}
