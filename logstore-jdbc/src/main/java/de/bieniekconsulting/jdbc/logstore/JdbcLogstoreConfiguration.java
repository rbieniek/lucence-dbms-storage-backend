package de.bieniekconsulting.jdbc.logstore;

import static org.springframework.context.annotation.FilterType.ANNOTATION;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;

import de.bieniekconsulting.springframework.support.TestConfiguration;

@Configuration
@ComponentScan(excludeFilters = @Filter(type = ANNOTATION, classes = TestConfiguration.class))
public class JdbcLogstoreConfiguration {

}
