package org.nusco.narjillos.views;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import org.nusco.narjillos.pond.Ecosystem;
import org.nusco.narjillos.pond.PondEventListener;
import org.nusco.narjillos.shared.things.Thing;

public class EcosystemView {

	private final Ecosystem ecosystem;
	private final Viewport viewport;
	private final Shape background;
	private final Map<Thing, ThingView> thingsToViews = new HashMap<>();

	private boolean infrared = false;

	public EcosystemView(Ecosystem ecosystem) {
		this.ecosystem = ecosystem;
		viewport = new Viewport(ecosystem);
		background = new Rectangle(0, 0, ecosystem.getSize(), ecosystem.getSize());

		for (Thing thing : ecosystem.getThings())
			addThingView(thing);

		ecosystem.addEventListener(new PondEventListener() {
			@Override
			public void thingAdded(Thing thing) {
				addThingView(thing);
			}

			@Override
			public void thingRemoved(Thing thing) {
				removeThingView(thing);
			}
		});
	}

	public Viewport getViewport() {
		return viewport;
	}

	public Node toNode() {
		Group result = new Group();
		result.getChildren().add(getBackground(isInfrared()));
		result.getChildren().add(getThingsGroup(isInfrared()));
		return result;
	}

	private Group getThingsGroup(boolean infraredOn) {
		Group things = new Group();
		things.getChildren().addAll(getNodesForThingsInOrder(infraredOn));

		things.getTransforms().add(new Translate(-viewport.getPositionPC().x, -viewport.getPositionPC().y));
		things.getTransforms().add(new Scale(viewport.getZoomLevel(), viewport.getZoomLevel(),
											viewport.getPositionPC().x, viewport.getPositionPC().y));

		setZoomLevelEffects(things);
		
		return things;
	}

	private void setZoomLevelEffects(Group group) {
		double zoomLevel = viewport.getZoomLevel();
		
		if (zoomLevel <= 1)
			return;
		
		group.setEffect(getBlurEffect(zoomLevel));
	}

	private Shape getBackground(boolean infraredOn) {
		if (infraredOn) {
			background.setFill(Color.DARKGRAY.darker());
			return background;
		}

		background.setFill(Color.ANTIQUEWHITE);
		double brightnessAdjust = -viewport.getZoomLevel() / 5;
		background.setEffect(new ColorAdjust(0, 0, brightnessAdjust, 0));
		return background;
	}
	
	private List<Node> getNodesForThingsInOrder(boolean infraredOn) {
		List<Node> result = new LinkedList<>();
		addNodesFor("food_piece", result, infraredOn);
		addNodesFor("narjillo", result, infraredOn);
		return result;
	}

	private void addNodesFor(String thingLabel, List<Node> result, boolean infraredOn) {
		for (ThingView view : getThingViews()) {
			if (view.getThing().getLabel().equals(thingLabel)) {
			Node node = view.toNode(viewport, infraredOn);
			if (node != null)
				result.add(node);
			}
		}
	}

	private Effect getBlurEffect(double zoomLevel) {
		int blurAmount = (int)(15 * (zoomLevel - 0.7));
		return new BoxBlur(blurAmount, blurAmount, 1);
	}

	private synchronized Collection<ThingView> getThingViews() {
		return new HashSet<ThingView>(thingsToViews.values());
	}

	private synchronized ThingView addThingView(Thing thing) {
		return thingsToViews.put(thing, ThingView.createViewFor(thing));
	}

	private synchronized void removeThingView(Thing thing) {
		thingsToViews.remove(thing);
	}

	public void tick() {
		viewport.tick();
	}

	public Ecosystem getPond() {
		return ecosystem;
	}

	public synchronized void toggleInfrared() {
		infrared = !infrared;
	}

	private synchronized boolean isInfrared() {
		return infrared;
	}
}