package org.nusco.swimmers.creature.body;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nusco.swimmers.shared.physics.Vector;
import org.nusco.swimmers.shared.utilities.ColorByte;

public class BodySegmentTest extends OrganTest {
	private BodyPart parent;
	
	@Override
	public BodyPart createConcreteBodyPart(int length, int thickness) {
		parent = new Head(10, 5, new ColorByte(100), 1);
		return new BodySegment(20, 10, 10, new ColorByte(100), parent, 0);
	}

	@Override
	public void hasAParent() {
		assertEquals(parent, part.getParent());
	}

	@Test
	public void startsAtItsParentsEndPoint() {
		assertEquals(parent.getEndPoint(), part.getStartPoint());
	}
	
	@Test
	public void hasAnAbsoluteAngle() {
		Head head = new Head(0, 0, new ColorByte(100), 1);
		BodyPart organ1 = new BodySegment(0, 0, 30, new ColorByte(100), head, 0);
		Organ organ2 = new BodySegment(0, 0, -10, new ColorByte(100), organ1, 0);
		assertEquals(20, organ2.getAbsoluteAngle(), 0);
	}

	@Override
	public void hasAnEndPoint() {
		Head head = new Head(10, 0, new ColorByte(100), 1);
		BodyPart organ1 = head.sproutOrgan(10, 0, 90, new ColorByte(100), 0);
		Organ organ2 = organ1.sproutOrgan(10, 0, -90, new ColorByte(100), 0);
		assertEquals(Vector.cartesian(20, 10), organ2.getEndPoint());
	}
}
