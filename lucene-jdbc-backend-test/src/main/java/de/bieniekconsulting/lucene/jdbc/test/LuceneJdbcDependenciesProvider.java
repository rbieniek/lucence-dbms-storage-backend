package de.bieniekconsulting.lucene.jdbc.test;

import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;

public class LuceneJdbcDependenciesProvider {

	public static Set<JavaArchive> dependencies() {
		final Set<JavaArchive> dependencies = new HashSet<>();
		final MavenResolverSystem resolver = Maven.resolver();

		for (final JavaArchive archive : resolver
				.resolve("org.apache.commons:commons-lang3:3.5", "org.springframework:spring-beans:4.3.7.RELEASE",
						"org.springframework:spring-core:4.3.7.RELEASE",
						"org.springframework:spring-context:4.3.7.RELEASE",
						"org.springframework:spring-context-support:4.3.7.RELEASE",
						"org.springframework:spring-jdbc:4.3.7.RELEASE", "org.springframework:spring-tx:4.3.7.RELEASE",
						"org.apache.commons:commons-io:1.3.2", "org.apache.commons:commons-compress:1.12",
						"org.apache.lucene:lucene-core:6.4.1", "org.apache.lucene:lucene-analyzers-common:6.4.1",
						"org.apache.lucene:lucene-codecs:6.4.1", "org.apache.lucene:lucene-expressions:6.4.1",
						"org.apache.lucene:lucene-facet:6.4.1", "org.apache.lucene:lucene-grouping:6.4.1",
						"org.apache.lucene:lucene-join:6.4.1", "org.apache.lucene:lucene-misc:6.4.1",
						"org.apache.lucene:lucene-queries:6.4.1", "org.apache.lucene:lucene-queryparser:6.4.1")
				.withTransitivity().as(JavaArchive.class)) {
			dependencies.add(archive);
		}

		return dependencies;
	}
}
