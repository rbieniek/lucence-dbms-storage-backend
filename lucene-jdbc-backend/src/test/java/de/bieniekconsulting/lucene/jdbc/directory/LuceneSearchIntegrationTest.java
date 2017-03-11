package de.bieniekconsulting.lucene.jdbc.directory;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.lucene.jdbc.directory.JdbcDirectory;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceConfiguration;
import de.bieniekconsulting.springframework.support.TestConfiguration;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LuceneSearchIntegrationTest.TestConfig.class)
public class LuceneSearchIntegrationTest {

	@Autowired
	private JdbcDirectory jdbcDirectory;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Before
	public void init() {
		jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
			stmt.execute("delete from LUCENE_LOCKS");
			return null;
		});

		jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
			stmt.execute("delete from LUCENE_EXTENTS");
			return null;
		});

		jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
			stmt.execute("delete from LUCENE_FILES");
			return null;
		});

		jdbcDirectory.setDirectoryName("outDir");
	}

	@Test
	public void shouldExecuteSimpleSearchInDatabase() throws Exception {
		final Analyzer analyzer = new StandardAnalyzer();

		// To store an index on disk, use this instead:
		final IndexWriterConfig config = new IndexWriterConfig(analyzer);

		final IndexWriter iwriter = new IndexWriter(jdbcDirectory, config);
		final Document doc = new Document();
		final String text = "This is the text to be indexed.";
		doc.add(new Field("fieldname", text, TextField.TYPE_STORED));
		iwriter.addDocument(doc);
		iwriter.close();

		// Now search the index:
		final DirectoryReader ireader = DirectoryReader.open(jdbcDirectory);
		final IndexSearcher isearcher = new IndexSearcher(ireader);
		// Parse a simple query that searches for "text":
		final QueryParser parser = new QueryParser("fieldname", analyzer);
		final Query query = parser.parse("text");
		final ScoreDoc[] hits = isearcher.search(query, 100).scoreDocs;
		assertThat(hits.length).isEqualTo(1);

		// Iterate through the results:
		for (final ScoreDoc hit : hits) {
			final Document hitDoc = isearcher.doc(hit.doc);

			assertThat(hitDoc.get("fieldname")).isEqualTo("This is the text to be indexed.");
		}
		ireader.close();
		jdbcDirectory.close();
	}

	@Test
	public void shouldExecuteTokenWithIdSearchInDatabase() throws Exception {
		final Analyzer analyzer = new StandardAnalyzer();

		final char[] alphas = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
				'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

		// To store an index on disk, use this instead:
		final IndexWriterConfig config = new IndexWriterConfig(analyzer);

		final IndexWriter iwriter = new IndexWriter(jdbcDirectory, config);

		int count = 0;

		for (int l1 = 0; l1 < 26; l1++) {
			for (int l2 = 0; l2 < 26; l2++) {
				final Document doc = new Document();

				final StringBuffer buffer = new StringBuffer();

				for (int l3 = 0; l3 < 26; l3++) {
					buffer.append(alphas[l1]);
					buffer.append(alphas[l2]);
					buffer.append(alphas[l3]);
					buffer.append(' ');
				}

				final String line = buffer.toString();

				doc.add(new Field("fieldname", line, TextField.TYPE_NOT_STORED));
				doc.add(new StoredField("id", l1 * 26 + l2));
				iwriter.addDocument(doc);

				if (line.contains("ccc")) {
					count++;
				}
			}
		}

		iwriter.close();

		assertThat(count).isEqualTo(1);

		// Now search the index:
		final DirectoryReader ireader = DirectoryReader.open(jdbcDirectory);
		final IndexSearcher isearcher = new IndexSearcher(ireader);
		// Parse a simple query that searches for "text":
		final QueryParser parser = new QueryParser("fieldname", analyzer);
		final Query query = parser.parse("ccc");
		final ScoreDoc[] hits = isearcher.search(query, 100).scoreDocs;
		assertThat(hits.length).isEqualTo(1);

		// Iterate through the results:
		for (final ScoreDoc hit : hits) {
			final Document hitDoc = isearcher.doc(hit.doc);

			assertThat(hitDoc.getField("id").numericValue()).isEqualTo(54);
		}
		ireader.close();
		jdbcDirectory.close();
	}

	@Test
	public void shouldExecuteSimpleSearchInMemory() throws Exception {
		final Analyzer analyzer = new StandardAnalyzer();

		// To store an index on disk, use this instead:
		// Directory directory = FSDirectory.open("/tmp/testindex");
		final Directory directory = new RAMDirectory();
		final IndexWriterConfig config = new IndexWriterConfig(analyzer);

		final IndexWriter iwriter = new IndexWriter(directory, config);
		final Document doc = new Document();
		final String text = "This is the text to be indexed.";
		doc.add(new Field("fieldname", text, TextField.TYPE_STORED));
		iwriter.addDocument(doc);
		iwriter.close();

		// Now search the index:
		final DirectoryReader ireader = DirectoryReader.open(directory);
		final IndexSearcher isearcher = new IndexSearcher(ireader);
		// Parse a simple query that searches for "text":
		final QueryParser parser = new QueryParser("fieldname", analyzer);
		final Query query = parser.parse("text");
		final ScoreDoc[] hits = isearcher.search(query, 100).scoreDocs;
		assertThat(hits.length).isEqualTo(1);

		// Iterate through the results:
		for (final ScoreDoc hit : hits) {
			final Document hitDoc = isearcher.doc(hit.doc);

			assertThat(hitDoc.get("fieldname")).isEqualTo("This is the text to be indexed.");
		}
		ireader.close();
		directory.close();
	}

	@TestConfiguration
	@Import({ LucenceConfiguration.class, CommonTestConfiguration.class })
	public static class TestConfig {
		@Bean
		@Autowired
		public JdbcTemplate jdbcTemplate(final DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

	}
}
