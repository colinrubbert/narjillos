package org.nusco.swimmers.genetics;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class DNAParserTest {

	@Test
	public void iteratesOverRightSizedParts() {
		int[] genes =  new int[]{
				1, 2, 3, 4,
				5, 6, 7, 8
			};

		DNA dna = new DNA(genes);
		DNAParser parser = new DNAParser(dna);
		
		assertArrayEquals(new int[]{1, 2, 3, 4}, parser.next());
		assertArrayEquals(new int[]{5, 6, 7, 8}, parser.next());
	}

	@Test
	public void padsShortSizedPartsToTheRightLength() {
		int[] genes =  new int[]{
				1, 2, 3, DNA.TERMINATE_PART,
				5, 6, 7, 8
			};

		DNA dna = new DNA(genes);
		DNAParser parser = new DNAParser(dna);
		
		assertArrayEquals(new int[]{1, 2, 3, 0}, parser.next());
		assertArrayEquals(new int[]{5, 6, 7, 8}, parser.next());
	}

	@Test
	public void padsEmptyPartsToTheRightLength() {
		int[] genes =  new int[]{
				DNA.TERMINATE_PART,
				1, 2, 3, 4
			};

		DNA dna = new DNA(genes);
		DNAParser parser = new DNAParser(dna);
		
		assertArrayEquals(new int[]{0, 0, 0, 0}, parser.next());
		assertArrayEquals(new int[]{1, 2, 3, 4}, parser.next());
	}

	@Test
	public void acceptsAnUnterminatedLastPart() {
		int[] genes =  new int[]{
				1, 2, 3, 4,
				5, 6, 7
			};

		DNA dna = new DNA(genes);
		DNAParser parser = new DNAParser(dna);
		
		assertArrayEquals(new int[]{1, 2, 3, 4}, parser.next());
		assertArrayEquals(new int[]{5, 6, 7, 0}, parser.next());
	}
}
