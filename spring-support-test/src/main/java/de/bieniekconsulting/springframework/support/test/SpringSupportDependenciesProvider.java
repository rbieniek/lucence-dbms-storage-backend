package de.bieniekconsulting.springframework.support.test;

import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;

public class SpringSupportDependenciesProvider {

	public static Set<JavaArchive> dependencies() {
		final Set<JavaArchive> dependencies = new HashSet<>();
		final MavenResolverSystem resolver = Maven.resolver();

		for (final JavaArchive archive : resolver
				.resolve("org.apache.commons:commons-lang3:3.5", "org.springframework:spring-beans:4.3.7.RELEASE",
						"org.springframework:spring-core:4.3.7.RELEASE",
						"org.springframework:spring-context:4.3.7.RELEASE",
						"org.springframework:spring-context-support:4.3.7.RELEASE")
				.withTransitivity().as(JavaArchive.class)) {
			dependencies.add(archive);
		}

		return dependencies;
	}
}
