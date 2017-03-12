package de.bieniekconsulting.logstore.springboot.components;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.logstore.springboot.types.LogstoreMessage;
import de.bieniekconsulting.logstore.springboot.types.ValidLogstoreMessage;

@Component
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
