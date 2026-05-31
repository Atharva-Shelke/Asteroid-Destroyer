package com.asteroids.model;

import java.util.Random;

import com.asteroids.util.PolygonFactory;

public class Asteroid extends Character {

	private double rotationalMovement;
	private static final Random RANDOM = new Random();

	public Asteroid(int x, int y) {
		super(PolygonFactory.createPolygon(), x, y);

		super.getShape().setRotate(RANDOM.nextInt(360));

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
