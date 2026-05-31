package com.asteroids.util;

public final class Constants {
	private Constants() {
		// private constructor to make it constant
	}

	public static final class Size {
		private Size() {
		}

		public static final int WIDTH = 600;
		public static final int HEIGHT = 600;
	}

	public static final class Value {
		private Value() {
		}

		public static final int INITIAL_ASTEROIDS = 5;
		public static final int MAX_PROJECTILES = 3;
		public static final int SCORE_PER_ASTEROID = 1000;
		public static final double ASTEROID_SPAWN_CHANCE = 0.005;
		public static final int PROJECTILE_SPEED = 3;
	}

	public static final class Turn {
		private Turn() {
		}

		public static final int LEFT = -5;
		public static final int RIGHT = 5;
	}

}
