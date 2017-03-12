package de.bieniekconsulting.logstore.wildfly.beans;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;
import de.bieniekconsulting.logstore.wildfly.types.ValidLogstoreMessage;

public class ValidLogstoreMessageValidator implements ConstraintValidator<ValidLogstoreMessage, LogstoreMessage> {

	@Override
	public void initialize(final ValidLogstoreMessage constraintAnnotation) {
		// Intenionally left out
	}

	@Override
	public boolean isValid(final LogstoreMessage value, final ConstraintValidatorContext context) {
		if (StringUtils.isBlank(value.getCompressedEncodedMessageText())
				&& StringUtils.isBlank(value.getMessageText())) {
			return false;
		}

		if (StringUtils.isNotBlank(value.getCompressedEncodedMessageText())
				&& StringUtils.isNotBlank(value.getMessageText())) {
			return false;
		}

		return true;
	}
}
