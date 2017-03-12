package de.bieniekconsulting.logstore.wildfly.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import de.bieniekconsulting.jdbc.logstore.JdbcLogstoreConfiguration;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceConfiguration;

@Configuration
@Import({ LucenceConfiguration.class, JdbcLogstoreConfiguration.class })
public class WildflySpringConfiguration {

}
