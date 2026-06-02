package com.asteroids.model;

import java.util.Random;

import com.asteroids.util.PolygonFactory;

import javafx.scene.paint.Color;

public class Asteroid extends Character {

	private double rotationalMovement;
	private static final Random RANDOM = new Random();

	public Asteroid(int x, int y) {
		super(PolygonFactory.createPolygon(), x, y);

		getShape().setRotate(RANDOM.nextInt(360));
		getShape().setFill(Color.SIENNA);
		getShape().setStroke(Color.SADDLEBROWN);
		getShape().setStrokeWidth(5);

		int accelerationAmount = 1 + RANDOM.nextInt(10);
		for (int i = 0; i < accelerationAmount; i++) {
			accelerate();
		}

		this.rotationalMovement = 0.5 - RANDOM.nextDouble();
	}

	@Override
	public void move() {
		super.move();
		super.getShape().setRotate(super.getShape().getRotate() + rotationalMovement);
	}
}
