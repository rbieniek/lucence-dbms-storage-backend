package de.bieniekconsulting.logstore.types;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import de.bieniekconsulting.logstore.components.ValidLogstoreMessageValidator;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Constraint(validatedBy = ValidLogstoreMessageValidator.class)
public @interface ValidLogstoreMessage {

	String message() default "Invalid Logstore message";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
