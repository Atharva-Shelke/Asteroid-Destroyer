package com.asteroids.model;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class Ship extends Character {

	public Ship(int x, int y) {
		super(new Polygon(-5, -5, 10, 0, -5, 5), x, y);
		getShape().setFill(Color.LIMEGREEN);

		super.setMovement(new Point2D(0.5, 0.0));
		super.move();
	}

}
