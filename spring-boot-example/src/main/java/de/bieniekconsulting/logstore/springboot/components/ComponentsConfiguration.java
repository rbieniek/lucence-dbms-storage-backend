package de.bieniekconsulting.logstore.springboot.components;

import static org.springframework.context.annotation.FilterType.ANNOTATION;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;

import de.bieniekconsulting.springframework.support.TestConfiguration;

import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(excludeFilters = @Filter(type = ANNOTATION, classes = TestConfiguration.class))
public class ComponentsConfiguration {

}
