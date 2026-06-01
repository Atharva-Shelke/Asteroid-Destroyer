package com.asteroids.model;

import com.asteroids.util.Constants;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public abstract class Character {

	private final Polygon shape;
	private Point2D movement;
	private boolean alive;

	public Character(Polygon polygon, int x, int y) {
		this.shape = polygon;
		this.shape.setTranslateX(x);
		this.shape.setTranslateY(y);

		this.movement = new Point2D(0, 0);

		this.alive = true;
	}

	public Polygon getShape() {
		return shape;
	}

	public void turnLeft() {
		this.shape.setRotate(this.shape.getRotate() + Constants.Turn.LEFT);
	}

	public void turnRight() {
		this.shape.setRotate(this.shape.getRotate() + Constants.Turn.RIGHT);
	}

	public void move() {
		this.shape.setTranslateX(this.shape.getTranslateX() + this.movement.getX());
		this.shape.setTranslateY(this.shape.getTranslateY() + this.movement.getY());

		if (this.shape.getTranslateX() < 0) {
			this.shape.setTranslateX(this.shape.getTranslateX() + Constants.Size.WIDTH);
		}

		if (this.shape.getTranslateX() > Constants.Size.WIDTH) {
			this.shape.setTranslateX(this.shape.getTranslateX() % Constants.Size.WIDTH);
		}

		if (this.shape.getTranslateY() < 0) {
			this.shape.setTranslateY(this.shape.getTranslateY() + Constants.Size.HEIGHT);
		}

		if (this.shape.getTranslateY() > Constants.Size.HEIGHT) {
			this.shape.setTranslateY(this.shape.getTranslateY() % Constants.Size.HEIGHT);
		}
	}

	public void accelerate() {
		double changeX = Math.cos(Math.toRadians(this.shape.getRotate()));
		double changeY = Math.sin(Math.toRadians(this.shape.getRotate()));

		changeX *= 0.05;
		changeY *= 0.05;

		this.movement = this.movement.add(changeX, changeY);
	}

	public boolean collide(Character other) {
		Shape collisionArea = Shape.intersect(this.shape, other.getShape());
		return collisionArea.getBoundsInLocal().getWidth() != -1;
	}

	public Point2D getMovement() {
		return movement;
	}

	public void setMovement(Point2D movement) {
		this.movement = movement;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

}
