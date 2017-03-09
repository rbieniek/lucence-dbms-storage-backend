package de.bieniekconsulting.logstore.lucene.jdbc.directory;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.bieniekconsulting.logstore.lucene.jdbc.types.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class LucenceService implements InitializingBean, DisposableBean {

	private static final String FIELD_MESSAGE = "message";
	private static final String FIELD_ID = "id";

	private final JdbcDirectory jdbcDirectory;

	private IndexWriter indexWriter;
	private AtomicInteger documentCounter = new AtomicInteger(0);
	private AtomicBoolean commitPending = new AtomicBoolean(false);

	public void indexLogRecord(final LogRecord record) throws IOException {
		final Document doc = new Document();

		doc.add(new Field(FIELD_MESSAGE, record.getMessageText(), TextField.TYPE_NOT_STORED));
		doc.add(new StoredField(FIELD_ID, record.getId()));
		try {
			indexWriter.addDocument(doc);

			if (documentCounter.incrementAndGet() % 512 == 0) {
				commitPending();
			} else {
				commitPending.set(true);
			}
		} catch (final IOException e) {
			log.info("cannot add record {} to search index", e);

			throw e;
		}

	}

	public void commitPending() throws IOException {
		if (commitPending.compareAndSet(true, false)) {
			indexWriter.commit();
		}
	}

	public Set<Long> searchLogRecords(final Set<String> searchTerms, final int searchLimit) throws IOException {
		final String queryExpr = searchTerms.stream().map(s -> StringUtils.containsWhitespace(s) ? "\"" + s + "\"" : s)
				.reduce((a, b) -> String.join(" AND ", a, b)).get();

		try {
			final DirectoryReader indexReader = DirectoryReader.open(jdbcDirectory);
			final IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			final StandardQueryParser queryParser = new StandardQueryParser(new WhitespaceAnalyzer());
			final Query query = queryParser.parse(queryExpr, FIELD_MESSAGE);
			final Set<Long> matchingIds = new HashSet<>();

			for (final ScoreDoc hit : indexSearcher.search(query, 100).scoreDocs) {
				final Document hitDoc = indexSearcher.doc(hit.doc);

				matchingIds.add(hitDoc.getField(FIELD_ID).numericValue().longValue());
			}

			return matchingIds;
		} catch (final QueryNodeException e) {
			log.info("Cannot query index with expression {}", queryExpr, e);

			throw new RuntimeException(e);
		} catch (final IOException e) {
			log.info("Cannot query search idnex", e);

			throw e;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jdbcDirectory.setDirectoryName("logstore-messages");

	}

	@EventListener(ContextRefreshedEvent.class)
	public void contextStarted() throws Exception {
		final IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());

		config.setOpenMode(CREATE_OR_APPEND);
		config.setMaxBufferedDocs(256);

		indexWriter = new IndexWriter(jdbcDirectory, config);
	}

	@Override
	public void destroy() throws Exception {
		if (indexWriter != null) {
			indexWriter.close();
		}

		jdbcDirectory.close();
	}
}
