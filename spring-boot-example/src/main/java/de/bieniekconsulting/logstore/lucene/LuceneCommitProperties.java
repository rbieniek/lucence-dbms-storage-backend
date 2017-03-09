package de.bieniekconsulting.logstore.lucene;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "lucene.commit")
@Getter
@Setter
public class LuceneCommitProperties {

	private int interval;
	private int limit;
}
