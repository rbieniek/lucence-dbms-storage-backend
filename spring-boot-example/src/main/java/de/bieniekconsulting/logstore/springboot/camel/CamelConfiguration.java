package de.bieniekconsulting.logstore.springboot.camel;

import static org.springframework.context.annotation.FilterType.ANNOTATION;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.component.servlet.ServletComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import de.bieniekconsulting.springframework.support.TestConfiguration;

@Configuration
@ComponentScan(excludeFilters = @Filter(type = ANNOTATION, classes = TestConfiguration.class))
public class CamelConfiguration {
	@Bean
	@Autowired
	public ServletRegistrationBean camelServletRegistration(final ServletComponent servletComponet) {
		final ServletRegistrationBean registration = new ServletRegistrationBean(new CamelHttpTransportServlet(),
				"/camel/*");

		registration.setName(servletComponet.getServletName());
		registration.setLoadOnStartup(1);

		return registration;
	}

	@Bean
	public LocalValidatorFactoryBean localValidatorFactoryBean() {
		return new LocalValidatorFactoryBean();
	}
}
