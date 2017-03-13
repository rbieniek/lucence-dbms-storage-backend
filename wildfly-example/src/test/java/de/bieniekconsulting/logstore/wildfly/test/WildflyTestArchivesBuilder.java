package de.bieniekconsulting.logstore.wildfly.test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;

import de.bieniekconsulting.jdbc.logstore.LogstoreJdbcDependenciesProvider;
import de.bieniekconsulting.jdbc.logstore.LogstoreJdbcTestJarBuilder;
import de.bieniekconsulting.logstore.wildfly.beans.ValidLogstoreMessageValidator;
import de.bieniekconsulting.logstore.wildfly.camel.LogstoreRouteBuilder;
import de.bieniekconsulting.logstore.wildfly.spring.WildflyApplicationContextProvider;
import de.bieniekconsulting.logstore.wildfly.spring.WildflySpringConfiguration;
import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;
import de.bieniekconsulting.lucene.jdbc.test.LuceneJdbcDependenciesProvider;
import de.bieniekconsulting.lucene.jdbc.test.LuceneJdbcTestJarBuilder;
import de.bieniekconsulting.springcdi.bridge.test.SpringCdiDependenciesProvider;
import de.bieniekconsulting.springcdi.bridge.test.SpringCdiTestJarBuilder;
import de.bieniekconsulting.springframework.support.test.SpringSupportDependenciesProvider;
import de.bieniekconsulting.springframework.support.test.SpringSupportTestJarBuilder;

public class WildflyTestArchivesBuilder {
	public static JavaArchive jar() {
		return ShrinkWrap.create(JavaArchive.class).addPackage(ValidLogstoreMessageValidator.class.getPackage())
				.addPackage(LogstoreRouteBuilder.class.getPackage())
				.addPackage(WildflySpringConfiguration.class.getPackage())
				.addPackage(LogstoreMessage.class.getPackage())
				.addAsManifestResource(new StringAsset(WildflyApplicationContextProvider.class.getName()),
						"services/de.bieniekconsulting.springcdi.bridge.support.ApplicationContextProvider")
				.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").addAsResource("db/changelog/db.changelog.xml");
	}

	public static WebArchive war() {
		final MavenResolverSystem resolver = Maven.resolver();
		final Set<JavaArchive> dependencies = new HashSet<>();

		dependencies.addAll(SpringCdiDependenciesProvider.dependencies());
		dependencies.addAll(SpringSupportDependenciesProvider.dependencies());
		dependencies.addAll(LuceneJdbcDependenciesProvider.dependencies());
		dependencies.addAll(LogstoreJdbcDependenciesProvider.dependencies());

		return ShrinkWrap.create(WebArchive.class).addAsLibraries(jar())
				.addAsLibraries(SpringCdiTestJarBuilder.extensionJar(), SpringSupportTestJarBuilder.jar(),
						LuceneJdbcTestJarBuilder.jar(), LogstoreJdbcTestJarBuilder.jar())
				.addAsLibraries(dependencies)
				.addAsLibraries(resolver
						.resolve("org.liquibase:liquibase-core:3.5.3", "org.apache.httpcomponents:httpcore:4.4.6",
								"org.apache.camel:camel-swagger-java:2.18.2", "org.apache.camel:camel-core:2.18.2",
								"org.apache.camel:camel-jackson:2.18.2", "org.apache.camel:camel-servlet:2.18.2",
								"org.apache.camel:camel-servletlistener:2.18.2")
						.withTransitivity().as(JavaArchive.class))
				.addAsWebInfResource("jboss-ds.xml")
				.setWebXML(new FileAsset(new File("src/main/webapp/WEB-INF/web.xml")));
	}
}
