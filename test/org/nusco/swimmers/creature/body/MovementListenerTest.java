package org.nusco.swimmers.creature.body;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.nusco.swimmers.shared.physics.Segment;
import org.nusco.swimmers.shared.physics.Vector;

public class MovementListenerTest {
	@Test
	public void wholeBodySendsMoveEvents() {
		Organ organ = new Head(1, 0, 0);
		Organ child1 = organ.sproutOrgan(2, 0, 0, 0);
		child1.sproutOrgan(3, 0, 0, 0);

		final int[] movement = new int[3];
		MovementListener listener = new MovementListener() {
			private int counter = 0;
			
			@Override
			public void moveEvent(Segment beforeMovement, Segment afterMovement) {
				movement[counter++] = (int)beforeMovement.vector.getLength();
			}
		};
		organ.setMovementListener(listener);
		
		organ.tick(Vector.ZERO);
		
		assertArrayEquals(new int[] {1, 2, 3}, movement);
	}
}
