package com.asteroids.util;

import javafx.scene.media.AudioClip;

public final class SoundPlayer {

	private SoundPlayer() {
	}

	private static final AudioClip SHOOT = new AudioClip(
			
			SoundPlayer.class.getResource("/sounds/shoot.wav").toExternalForm());

	private static final AudioClip EXPLOSION = new AudioClip(
			SoundPlayer.class.getResource("/sounds/explosion.wav").toExternalForm());

	private static final AudioClip GAME_OVER = new AudioClip(
			SoundPlayer.class.getResource("/sounds/gameover.wav").toExternalForm());

	public static void playShoot() {

		SHOOT.play();
	}

	public static void playExplosion() {
		EXPLOSION.play();
	}

	public static void playGameOver() {
		GAME_OVER.play();
	}
}