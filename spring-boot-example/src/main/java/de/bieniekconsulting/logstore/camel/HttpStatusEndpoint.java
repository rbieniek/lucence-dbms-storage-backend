package de.bieniekconsulting.logstore.camel;

import org.apache.camel.Exchange;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class HttpStatusEndpoint {
	public void methodNotAllowed(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.METHOD_NOT_ALLOWED.value());
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
		exchange.getOut().setBody(new byte[0]);
	}

	public void notFound(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.NOT_FOUND.value());
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.NOT_FOUND.getReasonPhrase());
		exchange.getOut().setBody(new byte[0]);
	}

	public void badRequest(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.BAD_REQUEST.value());
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.BAD_REQUEST.getReasonPhrase());
		exchange.getOut().setBody(new byte[0]);
	}

	public void internalServerError(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
		exchange.getOut().setBody(new byte[0]);
	}

	public void ok(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.OK.value());
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.OK.getReasonPhrase());
		exchange.getOut().setBody(new byte[0]);
	}

	public void created(final Exchange exchange) {
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.CREATED.value());
		exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_TEXT, HttpStatus.CREATED.getReasonPhrase());
		exchange.getOut().setBody(new byte[0]);
	}
}
