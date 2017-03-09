package de.bieniekconsulting.logstore.persistence;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "LOGSTORE_RECORDS")
@SequenceGenerator(name = LogstoreRecord.GENERATOR_NAME, sequenceName=LogstoreRecord.GENERATOR_NAME)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class LogstoreRecord implements Serializable {

	public static final String GENERATOR_NAME = "LOGSTORE_SEQ";

	/**
	 *
	 */
	private static final long serialVersionUID = 8804094301481660855L;

	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(generator = GENERATOR_NAME, strategy=GenerationType.SEQUENCE)
	@Setter(value = AccessLevel.PRIVATE)
	private Long id;

	@Column(name = "TIMESTAMP", nullable = false)
	private Long timestamp;

	@Column(name = "MESSAGE_TEXT", nullable = false)
	@Basic(fetch = FetchType.LAZY, optional = false)
	@Lob
	private String messageText;
}
