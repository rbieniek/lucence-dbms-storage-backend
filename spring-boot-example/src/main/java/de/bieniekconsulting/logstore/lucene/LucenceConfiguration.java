package de.bieniekconsulting.logstore.lucene;

import static org.springframework.context.annotation.FilterType.ANNOTATION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import de.bieniekconsulting.logstore.TestConfiguration;

@Configuration
@ComponentScan(excludeFilters = @Filter(type = ANNOTATION, classes = TestConfiguration.class))
@EnableConfigurationProperties({ LuceneCommitProperties.class })
public class LucenceConfiguration {

	@ConditionalOnProperty(prefix = "lucene.commit", name = "type", havingValue = "scheduler")
	@Bean
	@Autowired
	public MethodInvokingJobDetailFactoryBean jobDetails(final LucenceService service) {
		final MethodInvokingJobDetailFactoryBean jobDetails = new MethodInvokingJobDetailFactoryBean();

		jobDetails.setTargetObject(service);
		jobDetails.setTargetMethod("commitPending");

		return jobDetails;
	}

	@ConditionalOnProperty(prefix = "lucene.commit", name = "type", havingValue = "scheduler")
	@Bean
	@Autowired
	public SimpleTriggerFactoryBean trigger(final MethodInvokingJobDetailFactoryBean jobDetails,
			final LuceneCommitProperties properties) {
		final SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();

		trigger.setJobDetail(jobDetails.getObject());
		trigger.setStartDelay(properties.getInterval() * 60 * 1000L);
		trigger.setRepeatInterval(properties.getInterval() * 60 * 1000L);

		return trigger;
	}
}
