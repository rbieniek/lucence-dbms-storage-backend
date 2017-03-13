package de.bieniekconsulting.logstore.wildfly.camel;

import javax.inject.Inject;
import javax.validation.Validator;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;

public class ValidLogstoreMessagePredicate implements Predicate {

	@Inject
	private Validator validator;

	@Override
	public boolean matches(final Exchange exchange) {
		final LogstoreMessage message = exchange.getIn().getBody(LogstoreMessage.class);

		if (message == null) {
			return false;
		}

		return validator.validate(message).isEmpty();
	}

}
