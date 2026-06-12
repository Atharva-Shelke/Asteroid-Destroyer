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

	private static final AudioClip CRASH = new AudioClip(
			SoundPlayer.class.getResource("/sounds/crash.wav").toExternalForm());

	private static final AudioClip PAUSE = new AudioClip(
			SoundPlayer.class.getResource("/sounds/pause.mp3").toExternalForm());

	private static final AudioClip PLAY = new AudioClip(
			SoundPlayer.class.getResource("/sounds/play.wav").toExternalForm());

	public static void playShoot() {
		SHOOT.play();
	}

	public static void playExplosion() {
		EXPLOSION.play();
	}

	public static void playGameOver() {
		GAME_OVER.play();
	}

	public static void playCrash() {
		CRASH.play();
	}

	public static void playPause() {
		PAUSE.play();
	}

	public static void play() {
		PLAY.play();
	}

}