package de.bieniekconsulting.logstore.camel;

import javax.validation.Validator;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.logstore.types.LogstoreMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ValidLogstoreMessagePredicate implements Predicate {

	private final Validator validator;

	@Override
	public boolean matches(final Exchange exchange) {
		final LogstoreMessage message = exchange.getIn().getBody(LogstoreMessage.class);

		if (message == null) {
			return false;
		}

		return validator.validate(message).isEmpty();
	}

}
