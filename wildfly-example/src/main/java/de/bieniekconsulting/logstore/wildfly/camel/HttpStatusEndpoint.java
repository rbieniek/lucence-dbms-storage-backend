package de.bieniekconsulting.logstore.wildfly.camel;

import org.apache.camel.Exchange;
import org.apache.http.HttpStatus;

public class HttpStatusEndpoint {
	public void methodNotAllowed(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_METHOD_NOT_ALLOWED);
		exchange.getOut().setBody(new byte[0]);
	}

	public void notFound(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_NOT_FOUND);
		exchange.getOut().setBody(new byte[0]);
	}

	public void badRequest(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_BAD_REQUEST);
		exchange.getOut().setBody(new byte[0]);
	}

	public void internalServerError(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_INTERNAL_SERVER_ERROR);
		exchange.getOut().setBody(new byte[0]);
	}

	public void ok(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_OK);
		exchange.getOut().setBody(new byte[0]);
	}

	public void created(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_CREATED);
		exchange.getOut().setBody(new byte[0]);
	}
}
