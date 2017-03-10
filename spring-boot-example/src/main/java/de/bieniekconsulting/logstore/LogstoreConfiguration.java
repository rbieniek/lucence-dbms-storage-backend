package de.bieniekconsulting.logstore;

import static org.springframework.context.annotation.FilterType.ANNOTATION;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import de.bieniekconsulting.jdbc.logstore.JdbcLogstoreConfiguration;
import de.bieniekconsulting.springframework.support.TestConfiguration;

@SpringBootApplication
@Configuration
@ComponentScan(excludeFilters = @Filter(type = ANNOTATION, classes = TestConfiguration.class))
@EnableAutoConfiguration
@Import(JdbcLogstoreConfiguration.class)
public class LogstoreConfiguration {
	@Configuration
	@EnableWebSecurity
	public static class FormLoginSecurityConfig extends WebSecurityConfigurerAdapter {

		@Override
		public void configure(final WebSecurity web) throws Exception {
			web.ignoring().antMatchers("/camel/log/**");
		}

		@Override
		protected void configure(final HttpSecurity http) throws Exception {
			http.authorizeRequests().antMatchers("/camel/log/**").anonymous();
		}
	}
}
