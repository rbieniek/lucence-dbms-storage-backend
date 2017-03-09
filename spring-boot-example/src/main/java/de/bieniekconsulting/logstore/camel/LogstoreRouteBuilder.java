package de.bieniekconsulting.logstore.camel;

import static org.apache.camel.model.rest.RestParamType.body;
import static org.apache.camel.model.rest.RestParamType.path;
import static org.apache.camel.model.rest.RestParamType.query;

import java.io.IOException;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;

import de.bieniekconsulting.logstore.types.LogstoreMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class LogstoreRouteBuilder extends RouteBuilder {

	private final HttpStatusEndpoint httpStatusEndpoint;
	private final ValidLogstoreMessagePredicate validMessagePredicate;
	private final Base64CompressedMessageTextProcessor compressTextProcessor;
	private final PersistLogstoreMessageProcessor persistProcessor;
	private final IndexLogstoreRecordProcessor indexProcessor;
	private final RetrieveLogMessageEndpoint retrieveLogEndpoint;
	private final SearchLogMessageEndpoint searchLogEndpoint;

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

		rest("/log").get("/retrieve/{id}").produces(MediaType.APPLICATION_JSON_VALUE).outType(LogstoreMessage.class)
				.param().name("id").type(path).description("Log message ID").dataType("int").endParam()
				.responseMessage().code(HttpStatus.OK.value()).endResponseMessage().responseMessage()
				.code(HttpStatus.NOT_FOUND.value()).endResponseMessage().to("direct:log-get").put("/retrieve")
				.responseMessage().code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage()
				.to("direct:method-not-allowed").post("/retrieve").responseMessage()
				.code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage().to("direct:method-not-allowed")
				.delete("/retrieve").responseMessage().code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage()
				.to("direct:method-not-allowed").post("/store").consumes(MediaType.APPLICATION_JSON_VALUE)
				.type(LogstoreMessage.class).param().name("body").type(body)
				.description("Log messate to be stored and indexed").endParam().responseMessage()
				.code(HttpStatus.CREATED.value()).endResponseMessage().responseMessage()
				.code(HttpStatus.BAD_REQUEST.value()).endResponseMessage().to("direct:log-post").get("/store")
				.responseMessage().code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage()
				.to("direct:method-not-allowed").put("/store").responseMessage()
				.code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage().to("direct:method-not-allowed")
				.delete("/store").responseMessage().code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage()
				.to("direct:method-not-allowed").get("/search?term={term}&limit={limit}")
				.produces(MediaType.APPLICATION_JSON_VALUE).outType(LogstoreMessage.class).param().name("term")
				.type(query).description("Search term").dataType("string").endParam().param().name("limit").type(query)
				.description("Result set limit").dataType("int").endParam().responseMessage()
				.code(HttpStatus.OK.value()).endResponseMessage().to("direct:log-search").put("/search")
				.responseMessage().code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage()
				.to("direct:method-not-allowed").post("/search").responseMessage()
				.code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage().to("direct:method-not-allowed")
				.delete("/search").responseMessage().code(HttpStatus.METHOD_NOT_ALLOWED.value()).endResponseMessage()
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
