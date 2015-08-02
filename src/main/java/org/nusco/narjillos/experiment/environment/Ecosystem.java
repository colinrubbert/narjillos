package org.nusco.narjillos.experiment.environment;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.nusco.narjillos.core.physics.Segment;
import org.nusco.narjillos.core.physics.Vector;
import org.nusco.narjillos.core.things.FoodPiece;
import org.nusco.narjillos.core.things.Thing;
import org.nusco.narjillos.core.utilities.Configuration;
import org.nusco.narjillos.core.utilities.RanGen;
import org.nusco.narjillos.creature.Egg;
import org.nusco.narjillos.creature.Narjillo;
import org.nusco.narjillos.creature.body.physics.Viscosity;
import org.nusco.narjillos.creature.embryogenesis.Embryo;
import org.nusco.narjillos.genomics.DNA;
import org.nusco.narjillos.genomics.GenePool;

/**
 * A complex environment populate with narjillos, eggs and food.
 */
public class Ecosystem extends Environment {

	public static int numberOfBackgroundThreads = Runtime.getRuntime().availableProcessors();

	private final ExecutorService executorService;

	/** Counter used by the ThreadFactory to name threads. */
	private final AtomicInteger tickWorkerCounter = new AtomicInteger(1);

	private final Set<Narjillo> narjillos = new LinkedHashSet<>();

	private final Space space;
	private final Vector center;

	public Ecosystem(final long size, boolean sizeCheck) {
		super(size);
		
		ThreadFactory tickWorkerFactory = (Runnable r) -> {
			Thread result = new Thread(r, "tick-worker-" + tickWorkerCounter.getAndIncrement());
			result.setPriority(Thread.currentThread().getPriority());
			return result;
		};
		executorService = Executors.newFixedThreadPool(numberOfBackgroundThreads, tickWorkerFactory);

		this.space = new Space(size);
		this.center = Vector.cartesian(size, size).by(0.5);

		// check that things cannot move faster than a space area in a single
		// tick (which would make collision detection unreliable)
		if (sizeCheck && space.getAreaSize() < Viscosity.getMaxVelocity())
			throw new RuntimeException("Bug: Area size smaller than max velocity");
	}

	@Override
	public void tick(GenePool genePool, RanGen ranGen) {
		if (isShuttingDown())
			return; // we're leaving, apparently

		super.tick(genePool, ranGen);
	}
	
	@Override
	public Set<Thing> getThings(String label) {
		Set<Thing> result = new LinkedHashSet<>();
		// this ugliness will stay until we have narjillos
		// in the same space as other things
		if (label.equals("narjillo") || label.equals("")) {
			synchronized (narjillos) {
				result.addAll(narjillos);
			}
		}
		result.addAll(space.getAll(label));
		return result;
	}

	public Vector findClosestFoodPiece(Thing thing) {
		Thing target = space.findClosestTo(thing, "food_piece");

		if (target == null)
			return center;

		return target.getPosition();
	}

	public final FoodPiece spawnFood(Vector position) {
		FoodPiece newFood = new FoodPiece();
		newFood.setPosition(position);
		insert(newFood);
		return newFood;
	}

	public void insert(Thing thing) {
		space.add(thing);
		notifyThingAdded(thing);
	}

	public void insertNarjillo(Narjillo narjillo) {
		synchronized (narjillos) {
			narjillos.add(narjillo);
			notifyThingAdded(narjillo);
		}
	}

	public final Egg spawnEgg(DNA genes, Vector position, RanGen ranGen) {
		Egg egg = new Egg(genes, position, Vector.ZERO, Configuration.CREATURE_SEED_ENERGY, ranGen);
		insert(egg);
		return egg;
	}

	@Override
	public int getNumberOfFoodPieces() {
		return space.count("food_piece");
	}

	@Override
	public int getNumberOfEggs() {
		return space.count("egg");
	}

	@Override
	public int getNumberOfNarjillos() {
		synchronized (narjillos) {
			return narjillos.size();
		}
	}

	public Set<Narjillo> getNarjillos() {
		synchronized (narjillos) {
			return new LinkedHashSet<>(narjillos);
		}
	}

	public void updateTargets() {
		synchronized (narjillos) {
			narjillos.stream()
				.map((creature) -> (Narjillo) creature)
				.forEach((narjillo) -> {
					Vector closestTarget = findClosestFoodPiece(narjillo);
					narjillo.setTarget(closestTarget);
				});
		}
	}

	public void populate(String dna, GenePool genePool, RanGen ranGen) {
		populate(genePool, ranGen);
	}

	public void populate(GenePool genePool, RanGen ranGen) {
		spawnFood(ranGen);

		String dna1 = "{025_000_132_086_058_152_180_081_000_000_255_051}{128_001_158_115_194_203_232_065_128_128_128_000}{051_001_148_017_216_242_069_104_128_128_128_138}{093_001_163_025_137_181_086_015_128_128_128_245}{149_001_164_043_168_017_199_040_128_128_128_000}{086_001_245_011_031_136_189_234_128_128_128_009}{148_001_206_006_030_154_161_072_128_128_128_052}{128_001_238_026_021_124_115_093_128_128_128_063}{133_001_221_029_017_091_072_101_128_128_128_102}{023_001_009_095_137_041_255_081_128_128_128_012}{247_001_145_251_033_014_032_059_128_128_128_189}{065_001_049_018_160_144_055_101_128_128_128_062}{093_001_249_173_227_220_163_208_128_128_128_033}{071_001_243_210_139_176_153_221_128_128_128_045}{032_001_044_026_081_003_112_214_128_128_128_000}{125_001_227_110_033_000_124_088_128_128_128_100}";
		String dna2 = "{025_001_132_086_058_152_180_081_000_255_000_051}{128_001_158_115_194_203_232_065_128_128_128_000}{051_001_148_017_216_242_069_104_128_128_128_138}{093_001_163_025_137_181_086_015_128_128_128_245}{149_001_164_043_168_017_199_040_128_128_128_000}{086_001_245_011_031_136_189_234_128_128_128_009}{148_001_206_006_030_154_161_072_128_128_128_052}{128_001_238_026_021_124_115_093_128_128_128_063}{133_001_221_029_017_091_072_101_128_128_128_102}{023_001_009_095_137_041_255_081_128_128_128_012}{247_001_145_251_033_014_032_059_128_128_128_189}{065_001_049_018_160_144_055_101_128_128_128_062}{093_001_249_173_227_220_163_208_128_128_128_033}{071_001_243_210_139_176_153_221_128_128_128_045}{032_001_044_026_081_003_112_214_128_128_128_000}{125_001_227_110_033_000_124_088_128_128_128_100}";
		String dna3 = "{025_002_132_086_058_152_180_081_255_000_000_051}{128_001_158_115_194_203_232_065_128_128_128_000}{051_001_148_017_216_242_069_104_128_128_128_138}{093_001_163_025_137_181_086_015_128_128_128_245}{149_001_164_043_168_017_199_040_128_128_128_000}{086_001_245_011_031_136_189_234_128_128_128_009}{148_001_206_006_030_154_161_072_128_128_128_052}{128_001_238_026_021_124_115_093_128_128_128_063}{133_001_221_029_017_091_072_101_128_128_128_102}{023_001_009_095_137_041_255_081_128_128_128_012}{247_001_145_251_033_014_032_059_128_128_128_189}{065_001_049_018_160_144_055_101_128_128_128_062}{093_001_249_173_227_220_163_208_128_128_128_033}{071_001_243_210_139_176_153_221_128_128_128_045}{032_001_044_026_081_003_112_214_128_128_128_000}{125_001_227_110_033_000_124_088_128_128_128_100}";

		for (int i = 0; i < 100; i++) {
			spawnEgg(genePool.createDNA(dna1), randomPosition(getSize(), ranGen), ranGen);
		}

		for (int i = 0; i < 100; i++) {
			spawnEgg(genePool.createDNA(dna2), randomPosition(getSize(), ranGen), ranGen);
		}

		for (int i = 0; i < 100; i++) {
			spawnEgg(genePool.createDNA(dna3), randomPosition(getSize(), ranGen), ranGen);
		}
	}

	public synchronized void terminate() {
		executorService.shutdown();
		try {
			executorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean isShuttingDown() {
		return executorService.isShutdown();
	}

	protected Map<Narjillo, Future<Set<Thing>>> tickNarjillos(Set<Narjillo> narjillos) {
		Map<Narjillo, Future<Set<Thing>>> result = new LinkedHashMap<>();
		for (final Narjillo narjillo : narjillos) {
			result.put(narjillo, executorService.submit(() -> {
				Segment movement = narjillo.tick(getAtmosphere());
				return getCollisions(movement);
			}));
		}
		return result;
	}

	@Override
	protected void tickThings(GenePool genePool, RanGen ranGen) {
		new LinkedList<>(space.getAll("egg")).stream().forEach((thing) -> {
			tickEgg((Egg) thing, ranGen);
		});

		synchronized (narjillos) {
			new LinkedList<>(narjillos).stream()
				.filter((narjillo) -> (narjillo.isDead()))
				.forEach((narjillo) -> {
					removeNarjillo(narjillo, genePool);
				});
		}

		tickNarjillos(genePool, ranGen);

		if (shouldSpawnFood(ranGen)) {
			spawnFood(randomPosition(getSize(), ranGen));
			updateTargets();
		}

		synchronized (narjillos) {
			narjillos.stream().forEach((narjillo) -> {
				maybeLayEgg(narjillo, genePool, ranGen);
			});
		}
	}

	protected Set<Thing> getCollisions(Segment movement) {
		return space.detectCollisions(movement, "food_piece");
	}

	private void spawnFood(RanGen ranGen) {
		for (int i = 0; i < getNumberOf1000SquarePointsBlocks() * Configuration.ECOSYSTEM_FOOD_DENSITY_PER_BLOCK; i++)
			spawnFood(randomPosition(getSize(), ranGen));
	}

	private void tickEgg(Egg egg, RanGen ranGen) {
		egg.tick(getAtmosphere());
		if (egg.hatch(ranGen))
			insertNarjillo(egg.getHatchedNarjillo());
		if (egg.isDecayed())
			remove(egg);
	}

	private synchronized void tickNarjillos(GenePool genePool, RanGen ranGen) {
		Map<Narjillo, Set<Thing>> narjillosToCollidedFood;
		synchronized (narjillos) {
			narjillosToCollidedFood = tick(narjillos);
		}

		// Consume food in a predictable order, to avoid non-deterministic
		// behavior or race conditions when multiple narjillos collide with the
		// same piece of food.
		narjillosToCollidedFood.entrySet().stream()
			.forEach((entry) -> {
				Narjillo narjillo = entry.getKey();
				Set<Thing> collidedFood = entry.getValue();
				consume(narjillo, collidedFood, genePool, ranGen);
			});
	}

	private Map<Narjillo, Set<Thing>> tick(Set<Narjillo> narjillos) {
		Map<Narjillo, Set<Thing>> result = new LinkedHashMap<>();

		// Calculate collisions in parallel...
		Map<Narjillo, Future<Set<Thing>>> collisionFutures = tickNarjillos(narjillos);

		// ...but collect the results in a predictable sequential order
		for (Narjillo narjillo : collisionFutures.keySet()) {
			try {
				result.put(narjillo, collisionFutures.get(narjillo).get());
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		// Finally, go through the breathing loop (also sequential)
		for (Narjillo narjillo : narjillos)
			getAtmosphere().convert(narjillo.getBreathedElement(), narjillo.getByproduct());
		
		return result;
	}

	private boolean shouldSpawnFood(RanGen ranGen) {
		double maxFoodPieces = getNumberOf1000SquarePointsBlocks() * Configuration.ECOSYSTEM_MAX_FOOD_DENSITY_PER_1000_BLOCK;
		if (getNumberOfFoodPieces() >= maxFoodPieces)
			return false;

		double foodRespawnAverageInterval = Configuration.ECOSYSTEM_FOOD_RESPAWN_AVERAGE_INTERVAL_PER_BLOCK / getNumberOf1000SquarePointsBlocks();
		return ranGen.nextDouble() < 1.0 / foodRespawnAverageInterval;
	}

	private Vector randomPosition(long size, RanGen ranGen) {
		return Vector.cartesian(ranGen.nextDouble() * size, ranGen.nextDouble() * size);
	}

	private void updateTargets(Thing food) {
		synchronized (narjillos) {
			narjillos.stream()
				.filter((narjillo) -> (narjillo.getTarget().equals(food.getPosition())))
				.forEach((narjillo) -> {
					Vector closestTarget = findClosestFoodPiece(narjillo);
					narjillo.setTarget(closestTarget);
				});
		}
	}

	private void consume(Narjillo narjillo, Set<Thing> foodPieces, GenePool genePool, RanGen ranGen) {
		foodPieces.stream().forEach((foodPiece) -> {
			consumeFood(narjillo, (FoodPiece) foodPiece, genePool, ranGen);
		});
	}

	private void consumeFood(Narjillo narjillo, FoodPiece foodPiece, GenePool genePool, RanGen ranGen) {
		if (!space.contains(foodPiece))
			return;		// race condition: already consumed

		remove(foodPiece);
		narjillo.feedOn(foodPiece);

		updateTargets(foodPiece);
	}

	private void remove(Thing thing) {
		notifyThingRemoved(thing);
		space.remove(thing);
	}

	private void removeNarjillo(Narjillo narjillo, GenePool genePool) {
		notifyThingRemoved(narjillo);
		synchronized (narjillos) {
			narjillos.remove(narjillo);
		}
		genePool.remove(narjillo.getDNA());
	}

	private void maybeLayEgg(Narjillo narjillo, GenePool genePool, RanGen ranGen) {
		Egg egg = narjillo.layEgg(genePool, ranGen);
		if (egg == null)
			return;

		insert(egg);
	}

	private double getNumberOf1000SquarePointsBlocks() {
		double blocksPerEdge = getSize() / 1000.0;
		return blocksPerEdge * blocksPerEdge;
	}
}
