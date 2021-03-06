package org.nusco.narjillos.creature.body;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nusco.narjillos.core.chemistry.Element;
import org.nusco.narjillos.core.physics.Angle;
import org.nusco.narjillos.core.physics.Segment;
import org.nusco.narjillos.core.physics.Vector;
import org.nusco.narjillos.core.physics.ZeroVectorAngleException;
import org.nusco.narjillos.core.utilities.Configuration;
import org.nusco.narjillos.creature.body.physics.RotationsPhysicsEngine;
import org.nusco.narjillos.creature.body.physics.TranslationsPhysicsEngine;

/**
 * The physical body of a Narjillo, with all its organs and their position in
 * space.
 * 
 * This class contains the all-important Body.tick() method. Look at its
 * comments for details.
 */
public class Body {

	private final MovingOrgan head;
	private final double metabolicConsumption;
	private final double adultMass;
	private double mass;
	private double redMass;
	private double greenMass;
	private double blueMass;
	private transient List<ConnectedOrgan> organs;

	private transient Vector cachedCenterOfMass = null;
	private transient double cachedRadius = Double.NaN;

	public Body(MovingOrgan head) {
		this.head = head;
		adultMass = calculateAdultMass();
		this.metabolicConsumption = Math.pow(getHead().getMetabolicRate(), Configuration.PHYSICS_METABOLIC_CONSUMPTION_POW);
		updateMasses();
	}

	public Head getHead() {
		return (Head) head;
	}

	public List<ConnectedOrgan> getOrgans() {
		if (organs == null) {
			organs = new ArrayList<>();
			addWithChildren(organs, head);
		}
		return organs;
	}

	public Vector getStartPoint() {
		return getHead().getStartPoint();
	}

	public double getAngle() {
		return Angle.normalize(getHead().getAbsoluteAngle() + 180);
	}

	public double getMass() {
		return mass;
	}

	public double getGreenMass() {
		return greenMass;
	}

	public double getBlueMass() {
		return blueMass;
	}

	public double getRedMass() {
		return redMass;
	}

	public double getWaveBeatRatio() {
		return getHead().getWaveBeatRatio();
	}

	public Element getBreathedElement() {
		Element result = getBreathedElementFromFibers();

		// If a creature tries to cheat chemistry by turning an element
		// into the same element, then prevent it from breathing at all.
		if (result == getByproduct())
			return Element.ZERO;

		return result;
	}
	
	public double getEnergyToChildren() {
		return getHead().getEnergyToChildren();
	}

	public int getEggVelocity() {
		return getHead().getEggVelocity();
	}

	public int getEggInterval() {
		return getHead().getEggInterval();
	}

	// Creatures with a prevalence of red, green and blue mass breathe oxygen,
	// hydrogen and nitrogen, respectively.
	private Element getBreathedElementFromFibers() {
		if (redMass > greenMass && redMass > blueMass)
			return Element.OXYGEN;
		if (greenMass > blueMass)
			return Element.HYDROGEN;
		return Element.NITROGEN;
	}

	public Element getByproduct() {
		return getHead().getByproduct();
	}

	public double getAdultMass() {
		return adultMass;
	}

	public boolean hasStoppedGrowing() {
		return mass >= getAdultMass();
	}

	public synchronized double getRadius() {
		if (Double.isNaN(cachedRadius))
			cachedRadius = calculateRadius(getCenterOfMass());
		return cachedRadius;
	}

	public synchronized Vector getCenterOfMass() {
		if (cachedCenterOfMass == null)
			cachedCenterOfMass = calculateCenterOfMass();
		return cachedCenterOfMass;
	}

	public void forcePosition(Vector position, double angle) {
		getHead().forcePosition(position, angle);
		resetCaches();
	}

	/**
	 * Contains the core movement algorithm:
	 * 
	 * Take a target direction. Reposition the body's organs based on the target
	 * direction. Calculate the forces generated by the organs' movement. Use
	 * those forces to move the body in space. Return the energy consumed on the
	 * entire operation.
	 * 
	 * Look inside for more details...
	 */
	public double tick(Vector targetDirection) {
		// Before any movement, store away the current center of mass and the
		// angles and positions of all body parts. These will come useful later.
		// (Note that we could calculate the angles from the positions, but
		// computing angles is expensive - so it's faster to store the angles
		// away now that we already have them).
		Vector initialCenterOfMass = calculateCenterOfMass();
		Map<Organ, Double> initialAnglesOfOrgans = calculateAnglesOfOrgans();
		Map<Organ, Segment> initialPositionsOfOrgans = calculatePositionsOfOrgans();

		// This first step happens as if the body where in a vacuum.
		// The organs in the body remodel their own geometry based on the
		// target's direction. They don't "think" were to go - they just
		// changes their positions *somehow*. Natural selection will eventually
		// favor movements that result in getting closer to the target.
		tick_step1_updateAngles(targetDirection);

		// The organs might have grown during the previous ticks.
		// Update the masses in a still-developing body. (Then stop
		// doing it once the body is fully grown, to spare performance).
		if (!hasStoppedGrowing())
			updateMasses();

		// Changing the angles in the body results in a rotational force.
		// Rotate the body to match the force. In other words, keep the body's
		// moment of inertia equal to zero.
		double rotationEnergy = tick_step2_rotate(initialAnglesOfOrgans, initialPositionsOfOrgans, initialCenterOfMass, mass);

		// The previous updates moved the center of mass. Remember, we're
		// in a vacuum - so the center of mass shouldn't move. Let's put it
		// back to its original position.
		// It's important to recalculate the center of mass here.
		// Otherwise, we will get the old, cached value from before the movement.
		tick_step3_recenter(initialCenterOfMass, calculateCenterOfMass());
		
		// Now we can finally move out of the "vacuum" reference system.
		// All the movements from the previous steps result in a different
		// body position in space, and this different position generates
		// translational forces. We can update the body position based on
		// these translations.
		double translationEnergy = tick_step4_translate(initialPositionsOfOrgans, initialCenterOfMass, mass);

		resetCaches();

		// We're done! Return the energy spent on the entire operation.
		return getEnergyConsumed(rotationEnergy, translationEnergy);
	}

	final void updateMasses() {
		mass = 0;
		redMass = 0;
		greenMass = 0;
		blueMass = 0;
		for (Organ organ : getOrgans()) {
			double organMass = organ.getMass();
			mass += organMass;
			redMass += organMass * organ.getFiber().getPercentOfRed();
			greenMass += organMass * organ.getFiber().getPercentOfGreen();
			blueMass += organMass * organ.getFiber().getPercentOfBlue();
		}
	}

	@Override
	public String toString() {
		return head.toString();
	}

	public double getBrainWaveAngle() {
		return getHead().getBrainWaveAngle();
	}

	public void growToAdultForm() {
		getHead().growToAdultFormWithChildren();
		resetCaches();
		updateMasses();
	}

	private synchronized void resetCaches() {
		cachedCenterOfMass = null;
		cachedRadius = Double.NaN;
	}

	private Vector calculateCenterOfMass() {
		if (mass <= 0)
			return getStartPoint();

		// do it in one swoop instead of creating a lot of
		// intermediate vectors

		List<ConnectedOrgan> organs = getOrgans();
		Vector[] weightedCentersOfMass = new Vector[organs.size()];
		Iterator<ConnectedOrgan> iterator = organs.iterator();
		for (int i = 0; i < weightedCentersOfMass.length; i++) {
			Organ organ = iterator.next();
			weightedCentersOfMass[i] = organ.getCenterOfMass().by(organ.getMass());
		}

		double totalX = 0;
		double totalY = 0;
		for (int i = 0; i < weightedCentersOfMass.length; i++) {
			totalX += weightedCentersOfMass[i].x;
			totalY += weightedCentersOfMass[i].y;
		}

		return Vector.cartesian(totalX / mass, totalY / mass);
	}

	private void tick_step1_updateAngles(Vector targetDirection) {
		double angleToTarget = getAngleTo(targetDirection);
		getHead().tick(angleToTarget);
	}

	private double tick_step2_rotate(Map<Organ, Double> initialAnglesOfOrgans, Map<Organ, Segment> initialPositions, Vector centerOfMass,
			double mass) {
		RotationsPhysicsEngine forceField = new RotationsPhysicsEngine(mass, calculateRadius(centerOfMass), centerOfMass);
		for (Organ bodyPart : getOrgans())
			forceField.registerMovement(initialAnglesOfOrgans.get(bodyPart), bodyPart.getAbsoluteAngle(), bodyPart.getPositionInSpace(),
					bodyPart.getMass());
		getHead().rotateBy(forceField.getRotation());
		return forceField.getEnergy();
	}

	private void tick_step3_recenter(Vector centerOfMassBeforeReshaping, Vector centerOfMassAfterReshaping) {
		Vector centerOfMassOffset = centerOfMassBeforeReshaping.minus(centerOfMassAfterReshaping);
		getHead().translateBy(centerOfMassOffset);
	}

	private double tick_step4_translate(Map<Organ, Segment> initialPositions, Vector centerOfMass, double mass) {
		TranslationsPhysicsEngine forceField = new TranslationsPhysicsEngine(mass);
		for (Organ bodyPart : getOrgans())
			forceField.registerMovement(initialPositions.get(bodyPart), bodyPart.getPositionInSpace(), bodyPart.getMass());
		getHead().translateBy(forceField.getTranslation());
		return forceField.getEnergy();
	}

	private double getEnergyConsumed(double rotationEnergy, double translationEnergy) {
		return (rotationEnergy + translationEnergy) * metabolicConsumption;
	}

	private void addWithChildren(List<ConnectedOrgan> result, MovingOrgan organ) {
		// children first
		for (ConnectedOrgan child : organ.getChildren())
			addWithChildren(result, (MovingOrgan) child);
		result.add(organ);
	}

	private double getAngleTo(Vector direction) {
		if (direction.equals(Vector.ZERO))
			return 0;
		try {
			return Angle.normalize(getAngle() - direction.getAngle());
		} catch (ZeroVectorAngleException e) {
			throw new RuntimeException(e); // should never happen
		}
	}

	private Map<Organ, Segment> calculatePositionsOfOrgans() {
		Map<Organ, Segment> result = new LinkedHashMap<>();
		for (Organ organ : getOrgans())
			result.put(organ, organ.getPositionInSpace());
		return result;
	}

	private Map<Organ, Double> calculateAnglesOfOrgans() {
		Map<Organ, Double> result = new LinkedHashMap<>();
		for (Organ organ : getOrgans())
			result.put(organ, organ.getAbsoluteAngle());
		return result;
	}

	private double calculateAdultMass() {
		double result = 0;
		for (Organ organ : getOrgans())
			result += organ.getAdultMass();
		return result;
	}

	private double calculateRadius(Vector centerOfMass) {
		final double MIN_RADIUS = 1;
		double result = MIN_RADIUS;
		for (Organ bodyPart : getOrgans()) {
			double startPointDistance = bodyPart.getStartPoint().minus(centerOfMass).getLength();
			double endPointDistance = bodyPart.getEndPoint().minus(centerOfMass).getLength();
			double distance = Math.max(startPointDistance, endPointDistance);
			if (distance > result)
				result = distance;
		}
		return result;
	}
}
