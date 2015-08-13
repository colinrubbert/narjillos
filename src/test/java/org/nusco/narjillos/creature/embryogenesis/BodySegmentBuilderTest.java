package org.nusco.narjillos.creature.embryogenesis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nusco.narjillos.creature.body.BodyPart;
import org.nusco.narjillos.creature.body.Fiber;
import org.nusco.narjillos.creature.body.Head;
import org.nusco.narjillos.creature.body.HeadParameters;
import org.nusco.narjillos.genomics.Chromosome;

public class BodySegmentBuilderTest extends ConcreteOrganBuilderTest {

	@Override
	protected BodySegmentBuilder getConcreteOrganBuilder(Chromosome chromosome) {
		return new BodySegmentBuilder(chromosome);
	}

	@Test
	public void decodesAFiberShift() {
		Chromosome chromosome1 = new Chromosome(0, 0, 0, 0, 0, 255, 1);
		assertEquals(-255, getConcreteOrganBuilder(chromosome1).getRedShift());
		assertEquals(255, getConcreteOrganBuilder(chromosome1).getGreenShift());
		assertEquals(-253, getConcreteOrganBuilder(chromosome1).getBlueShift());

		Chromosome chromosome2 = new Chromosome(0, 0, 0, 0, 126, 127, 128);
		assertEquals(-3, getConcreteOrganBuilder(chromosome2).getRedShift());
		assertEquals(-1, getConcreteOrganBuilder(chromosome2).getGreenShift());
		assertEquals(1, getConcreteOrganBuilder(chromosome2).getBlueShift());
	}

	@Test
	public void decodesAnAngleToTheParentBetweenMinus70And70() {
		assertEquals(-70, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0)).getAngleToParent(1));
		assertEquals(-69, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 1)).getAngleToParent(1));
		assertEquals(-68, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 2)).getAngleToParent(1));
		assertEquals(0, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 128)).getAngleToParent(1));
		assertEquals(1, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 129)).getAngleToParent(1));
		assertEquals(70, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 255)).getAngleToParent(1));
	}

	@Test
	public void mirrorsTheAngleToTheParentIfTheMirrorSignIsNegative() {
		assertEquals(70, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0)).getAngleToParent(-1));
		assertEquals(69, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 1)).getAngleToParent(-1));
		assertEquals(68, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 2)).getAngleToParent(-1));
		assertEquals(0, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 128)).getAngleToParent(-1));
		assertEquals(-1, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 129)).getAngleToParent(-1));
		assertEquals(-70, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 255)).getAngleToParent(-1));
	}

	@Test
	public void decodesADelayBetween0And30() {
		assertEquals(0, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0)).getDelay());
		assertEquals(0, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 8)).getDelay());
		assertEquals(1, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 9)).getDelay());
		assertEquals(30, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 255)).getDelay());
	}

	@Test
	public void decodesAnAmplitudeBetween1And100() {
		assertEquals(1, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0, 1)).getAmplitude());
		assertEquals(2, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0, 5)).getAmplitude());
		assertEquals(80, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0, 255)).getAmplitude());
	}

	@Test
	public void decodesASkewingBetweenMinus90And90() {
		assertEquals(-90, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)).getSkewing());
		assertEquals(-45, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 63)).getSkewing());
		assertEquals(0, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 127)).getSkewing());
		assertEquals(90, getConcreteOrganBuilder(new Chromosome(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 255)).getSkewing());
	}

	@Test
	public void buildsABodySegment() {
		int controlFlowGene = 0b00000000;
		int controlLoopGene = 0b00000000;
		int lengthGene = 30;
		int thicknessGene = 126;
		int redShiftGene = 126;
		int greenShiftGene = 127;
		int blueShiftGene = 128;
		int angleToParentGene = 81;
		int delayGene = 90;
		int amplitudeGene = 107;
		int skewingGene = 150;
		
		Chromosome chromosome = new Chromosome(controlFlowGene, controlLoopGene, lengthGene, thicknessGene, redShiftGene, greenShiftGene, blueShiftGene, angleToParentGene, delayGene, amplitudeGene, skewingGene);
		BodySegmentBuilder builder = getConcreteOrganBuilder(chromosome);
		
		HeadParameters headParameters = new HeadParameters();
		headParameters.setRed(50);
		headParameters.setGreen(60);
		headParameters.setBlue(70);
		Head head = new Head(headParameters);
		
		BodyPart bodyPart = (BodyPart) builder.buildOrgan(head, 1);

		head.updateGeometry();
		bodyPart.updateGeometry();
		
		assertEquals(new Fiber(47, 59, 71), bodyPart.getFiber());
		assertEquals(-25, bodyPart.getAbsoluteAngle(), 0);
		assertEquals(10, bodyPart.getDelay(), 0);
		assertEquals(34, bodyPart.getAmplitude(), 0);
		assertEquals(16, bodyPart.getSkewing(), 0);

		head.growToAdultFormWithChildren();
		bodyPart.growToAdultFormWithChildren();
		
		assertEquals(30, bodyPart.getLength(), 0);
		assertEquals(25, bodyPart.getThickness(), 0.01);
	}
}
