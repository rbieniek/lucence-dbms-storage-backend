package de.bieniekconsulting.logstore.wildfly.camel;

import static org.apache.camel.model.rest.RestParamType.body;
import static org.apache.camel.model.rest.RestParamType.path;
import static org.apache.camel.model.rest.RestParamType.query;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.core.JsonParseException;

import de.bieniekconsulting.logstore.wildfly.types.LogstoreMessage;

public class LogstoreRouteBuilder extends RouteBuilder {

	private static final String APPLICATION_JSON_VALUE = "application/json";

	@Inject
	private HttpStatusEndpoint httpStatusEndpoint;
	@Inject
	private ValidLogstoreMessagePredicate validMessagePredicate;
	@Inject
	private Base64CompressedMessageTextProcessor compressTextProcessor;
	@Inject
	private PersistLogstoreMessageProcessor persistProcessor;
	@Inject
	private IndexLogstoreRecordProcessor indexProcessor;
	@Inject
	private RetrieveLogMessageEndpoint retrieveLogEndpoint;
	@Inject
	private SearchLogMessageEndpoint searchLogEndpoint;

	@Override
	public void configure() throws Exception {
		restConfiguration("servlet").contextPath("/camel").bindingMode(RestBindingMode.json)
				.dataFormatProperty("prettyPrint", "true").apiContextPath("/api-doc")
				.apiProperty("api.title", "RESTful log store API").apiProperty("api.version", "1.0");

		onException(JsonParseException.class).handled(true).to("log:out?showException=true").bean(httpStatusEndpoint,
				"badRequest");
		onException(IOException.class).handled(true).to("log:out?showException=true").bean(httpStatusEndpoint,
				"internalServerError");
		onException(RuntimeException.class).handled(true).to("log:out?showException=true").bean(httpStatusEndpoint,
				"badRequest");

		rest("/log").get("/retrieve/{id}").produces(APPLICATION_JSON_VALUE).outType(LogstoreMessage.class).param()
				.name("id").type(path).description("Log message ID").dataType("int").endParam().responseMessage()
				.code(HttpStatus.SC_OK).endResponseMessage().responseMessage().code(HttpStatus.SC_NOT_FOUND)
				.endResponseMessage().to("direct:log-get").put("/retrieve").responseMessage()
				.code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage().to("direct:method-not-allowed")
				.post("/retrieve").responseMessage().code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage()
				.to("direct:method-not-allowed").delete("/retrieve").responseMessage()
				.code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage().to("direct:method-not-allowed")
				.post("/store").consumes(APPLICATION_JSON_VALUE).type(LogstoreMessage.class).param().name("body")
				.type(body).description("Log messate to be stored and indexed").endParam().responseMessage()
				.code(HttpStatus.SC_CREATED).endResponseMessage().responseMessage().code(HttpStatus.SC_BAD_REQUEST)
				.endResponseMessage().to("direct:log-post").get("/store").responseMessage()
				.code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage().to("direct:method-not-allowed")
				.put("/store").responseMessage().code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage()
				.to("direct:method-not-allowed").delete("/store").responseMessage()
				.code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage().to("direct:method-not-allowed")
				.get("/search?term={term}&limit={limit}").produces(APPLICATION_JSON_VALUE)
				.outType(LogstoreMessage.class).param().name("term").type(query).description("Search term")
				.dataType("string").endParam().param().name("limit").type(query).description("Result set limit")
				.dataType("int").endParam().responseMessage().code(HttpStatus.SC_OK).endResponseMessage()
				.to("direct:log-search").put("/search").responseMessage().code(HttpStatus.SC_METHOD_NOT_ALLOWED)
				.endResponseMessage().to("direct:method-not-allowed").post("/search").responseMessage()
				.code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage().to("direct:method-not-allowed")
				.delete("/search").responseMessage().code(HttpStatus.SC_METHOD_NOT_ALLOWED).endResponseMessage()
				.to("direct:method-not-allowed");

		from("direct:log-post").choice().when(bodyAs(LogstoreMessage.class).isNotNull()).to("direct:log-validate")
				.otherwise().bean(httpStatusEndpoint, "badRequest");

		from("direct:log-validate").choice().when(validMessagePredicate).process(compressTextProcessor)
				.to("direct:log-store").otherwise().bean(httpStatusEndpoint, "badRequest");

		from("direct:log-store").process(persistProcessor).process(indexProcessor).bean(httpStatusEndpoint, "created");

		from("direct:log-get").bean(retrieveLogEndpoint, "retrieveLogMessage");

		from("direct:log-search").bean(searchLogEndpoint, "searchLogMessages");

		from("direct:method-not-allowed").bean(httpStatusEndpoint, "methodNotAllowed");
	}

}
