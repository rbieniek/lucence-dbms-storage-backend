package de.bieniekconsulting.lucene.jdbc.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import de.bieniekconsulting.lucene.jdbc.directory.LucenceConfiguration;
import de.bieniekconsulting.lucene.jdbc.directory.LucenceService;
import de.bieniekconsulting.lucene.jdbc.types.LogRecord;
import de.bieniekconsulting.springframework.support.TestConfiguration;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LuceneServiceIntegrationTest.TestConfig.class)
public class LuceneServiceIntegrationTest {

	private final char[] alphas = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
			'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	@Autowired
	private LucenceService luceneService;

	@Test
	public void shouldFindOneWord() throws Exception {
		for (int l1 = 0; l1 < 26; l1++) {
			for (int l2 = 0; l2 < 26; l2++) {
				final StringBuffer buffer = new StringBuffer();

				for (int l3 = 0; l3 < 26; l3++) {
					buffer.append('a');
					buffer.append(alphas[l1]);
					buffer.append(alphas[l2]);
					buffer.append(alphas[l3]);
					buffer.append(' ');
				}

				final String line = buffer.toString();

				luceneService.indexLogRecord(LogRecord.builder().id((long) (l1 * 26 + l2)).messageText(line).build());
			}
		}

		final Set<String> searchTerms = new HashSet<>();

		searchTerms.add("accc");

		final Set<Long> matchingIds = luceneService.searchLogRecords(searchTerms, 100);

		assertThat(matchingIds).hasSize(1).containsExactly(54L);

	}

	@Test
	public void shouldFindTwoWordsSearchTerm() throws Exception {
		for (int l1 = 0; l1 < 26; l1++) {
			for (int l2 = 0; l2 < 26; l2++) {
				final StringBuffer buffer = new StringBuffer();

				for (int l3 = 0; l3 < 26; l3++) {
					buffer.append('b');
					buffer.append(alphas[l1]);
					buffer.append(alphas[l2]);
					buffer.append(alphas[l3]);
					buffer.append(' ');
				}

				final String line = buffer.toString();

				luceneService.indexLogRecord(LogRecord.builder().id((long) (l1 * 26 + l2)).messageText(line).build());
			}
		}

		final Set<String> searchTerms = new HashSet<>();

		searchTerms.add("bccc bccd");

		final Set<Long> matchingIds = luceneService.searchLogRecords(searchTerms, 100);

		assertThat(matchingIds).hasSize(1).containsExactly(54L);

	}

	@Test
	public void shouldFindTwoTermsWithOneWord() throws Exception {
		for (int l1 = 0; l1 < 26; l1++) {
			for (int l2 = 0; l2 < 26; l2++) {
				final StringBuffer buffer = new StringBuffer();

				for (int l3 = 0; l3 < 26; l3++) {
					buffer.append('c');
					buffer.append(alphas[l1]);
					buffer.append(alphas[l2]);
					buffer.append(alphas[l3]);
					buffer.append(' ');
				}

				final String line = buffer.toString();

				luceneService.indexLogRecord(LogRecord.builder().id((long) (l1 * 26 + l2)).messageText(line).build());
			}
		}

		final Set<String> searchTerms = new HashSet<>();

		searchTerms.add("cccc");
		searchTerms.add("cccd");

		final Set<Long> matchingIds = luceneService.searchLogRecords(searchTerms, 100);

		assertThat(matchingIds).hasSize(1).containsExactly(54L);

	}

	@Test
	public void shouldFindTwoTermsWithTwoWord() throws Exception {
		for (int l1 = 0; l1 < 26; l1++) {
			for (int l2 = 0; l2 < 26; l2++) {
				final StringBuffer buffer = new StringBuffer();

				for (int l3 = 0; l3 < 26; l3++) {
					buffer.append('d');
					buffer.append(alphas[l1]);
					buffer.append(alphas[l2]);
					buffer.append(alphas[l3]);
					buffer.append(' ');
				}

				final String line = buffer.toString();

				luceneService.indexLogRecord(LogRecord.builder().id((long) (l1 * 26 + l2)).messageText(line).build());
			}
		}

		final Set<String> searchTerms = new HashSet<>();

		searchTerms.add("dccc dccd");
		searchTerms.add("dcch dcci");

		final Set<Long> matchingIds = luceneService.searchLogRecords(searchTerms, 100);

		assertThat(matchingIds).hasSize(1).containsExactly(54L);

	}

	@TestConfiguration
	@Import({ LucenceConfiguration.class, CommonTestConfiguration.class })
	public static class TestConfig {

	}
}
