package de.bieniekconsulting.lucene.jdbc.types;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LuceneFileExtent {
	private UUID id;
	private UUID fileId;
	private int extentNumber;
	private int extentLength;
	private byte[] extentData;
}