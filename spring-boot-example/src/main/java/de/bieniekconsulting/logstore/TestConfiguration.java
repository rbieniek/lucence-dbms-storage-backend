package de.bieniekconsulting.logstore;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Configuration
public @interface TestConfiguration {

}
