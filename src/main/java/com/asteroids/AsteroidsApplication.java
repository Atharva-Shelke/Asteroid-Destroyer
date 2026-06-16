package com.asteroids;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.asteroids.model.Asteroid;
import com.asteroids.model.Projectile;
import com.asteroids.model.Ship;
import com.asteroids.util.Constants;
import com.asteroids.util.SoundPlayer;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AsteroidsApplication extends Application {

	// Initializations
	private static final Random RANDOM = new Random();
	private int invulnerabilityFrames = Constants.Value.INVULNERABILITY_FRAMES;

	// Game State
	private boolean paused = false;
	private int points = 0;
	private int score = 0;
	private int highScore;
	private int lives = 3;
	private int level = 1;

	// Game Objects
	private final Ship ship = new Ship(Constants.Size.WIDTH / 2, Constants.Size.HEIGHT / 2);
	private final List<Asteroid> asteroids = new ArrayList<>();
	private final List<Projectile> projectiles = new ArrayList<>();

	// HUD
	private final Text scoreText = new Text();
	private final Text pointsText = new Text();
	private final Text livesText = new Text();
	private final Text highText = new Text();
	private final Text levelText = new Text();

	// Overlays
	private VBox shipDestroyedBox;
	private final Text levelUpText = new Text();

	@Override
	public void start(Stage stage) throws Exception {

		Pane gameLayer = new Pane();

		Pane hudLayer = new Pane();
		hudLayer.setMouseTransparent(true);

		StackPane effectsLayer = new StackPane();
		effectsLayer.setMouseTransparent(true);

		StackPane overlayLayer = new StackPane();
		overlayLayer.setMouseTransparent(false);

		StackPane gameArea = new StackPane(gameLayer, hudLayer, effectsLayer, overlayLayer);

		gameLayer.setPrefSize(Constants.Size.WIDTH, Constants.Size.HEIGHT);
		gameLayer.setStyle("-fx-background-color: black;");

		highScore = loadHighScore();

		createHUD(hudLayer);

		createStarField(gameLayer);

		createLevelUp(effectsLayer);
		showLevelUp();

		gameLayer.getChildren().add(ship.getShape());

		createInitialAsteroids(gameLayer);

		Map<KeyCode, Boolean> pressedKeys = new HashMap<>();

		Scene scene = new Scene(gameArea);

		scene.getStylesheets().add(getClass().getResource("/style/game.css").toExternalForm());

		VBox pauseBox = pausedMenu();

		overlayLayer.getChildren().add(pauseBox);

		scene.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.SPACE) {
				if (paused) {
					event.consume();
				} else if (projectiles.size() < Constants.Value.MAX_PROJECTILES) {
					shootProjectile(gameLayer);
				}
			} else if (event.getCode() == KeyCode.ESCAPE) {

				paused = !paused;
				pauseBox.setVisible(paused);
				if (paused) {
					SoundPlayer.playPause();
				} else {
					SoundPlayer.play();
				}
				return;
			} else {
				pressedKeys.put(event.getCode(), Boolean.TRUE);
			}
		});

		scene.setOnKeyReleased(event -> {
			pressedKeys.put(event.getCode(), Boolean.FALSE);
		});

		new AnimationTimer() {

			@Override
			public void handle(long now) {

				if (paused) {
					return;
				}

				if (invulnerabilityFrames > 0) {
					ship.getShape().setOpacity(0.4);
					invulnerabilityFrames--;
				} else {
					ship.getShape().setOpacity(1.0);
				}

				handleInput(pressedKeys);

				moveObjects();

				if (invulnerabilityFrames <= 0 && shipCollided()) {
					paused = true;
					lives--;
					updateHUD();

					overlayLayer.getChildren().add(shipDestroyedMenu(overlayLayer, gameLayer));

					if (lives <= 0) {
						SoundPlayer.playGameOver();

					} else {
						SoundPlayer.playCrash();
					}
				}

				handleProjectileCollisions();

				removeDestroyedProjectiles(gameLayer);

				removeDestroyedAsteroids(gameLayer);

				spawnAsteroid(gameLayer);

			}
		}.start();

		SoundPlayer.play();

		stage.setTitle("Asteroids!");
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	private void createInitialAsteroids(Pane gameLayer) {
		for (int i = 0; i < Constants.Value.INITIAL_ASTEROIDS; i++) {
			Asteroid a = new Asteroid(RANDOM.nextInt(Constants.Size.WIDTH), RANDOM.nextInt(Constants.Size.HEIGHT));
			asteroids.add(a);
			gameLayer.getChildren().add(a.getShape());
		}
	}

	private void handleInput(Map<KeyCode, Boolean> pressedKeys) {

		if (pressedKeys.getOrDefault(KeyCode.LEFT, false)) {
			ship.turnLeft();
		}

		if (pressedKeys.getOrDefault(KeyCode.RIGHT, false)) {
			ship.turnRight();
		}

		if (pressedKeys.getOrDefault(KeyCode.UP, false)) {
			ship.accelerate();
		}
	}

	private void moveObjects() {

		ship.move();
		asteroids.forEach(Asteroid::move);
		projectiles.forEach(Projectile::move);
	}

	private boolean shipCollided() {

		return asteroids.stream().anyMatch(ship::collide);
	}

	private void handleProjectileCollisions() {

		projectiles.forEach(projectile -> {
			asteroids.forEach(asteroid -> {

				if (projectile.collide(asteroid)) {

					SoundPlayer.playExplosion();

					projectile.setAlive(false);
					asteroid.setAlive(false);

					asteroidDestroyed();

				}
			});
		});
	}

	private void removeDestroyedProjectiles(Pane gameLayer) {

		List<Projectile> destroyedP = projectiles.stream().filter(p -> !p.isAlive()).toList();

		destroyedP.forEach(p -> gameLayer.getChildren().remove(p.getShape()));

		projectiles.removeAll(destroyedP);
	}

	private void removeDestroyedAsteroids(Pane gameLayer) {

		List<Asteroid> destroyedA = asteroids.stream().filter(a -> !a.isAlive()).toList();

		destroyedA.forEach(a -> gameLayer.getChildren().remove(a.getShape()));

		asteroids.removeAll(destroyedA);
	}

	private void spawnAsteroid(Pane gameLayer) {

		double spawnChance = Constants.Value.ASTEROID_SPAWN_CHANCE + ((level - 1) * 0.002);
		if (RANDOM.nextDouble() < spawnChance) {

			Asteroid asteroid = new Asteroid(RANDOM.nextInt(Constants.Size.WIDTH),
					RANDOM.nextInt(Constants.Size.HEIGHT));

			double speedMultiplier = Math.min(2.0, 1.0 + ((level - 1) * 0.15));

			if (!asteroid.collide(ship)) {

				asteroids.add(asteroid);
				asteroid.setMovement(asteroid.getMovement().multiply(speedMultiplier));
				gameLayer.getChildren().add(asteroid.getShape());
			}
		}
	}

	private void createStarField(Pane gameLayer) {
		for (int i = 0; i < 100; i++) {
			Circle star = new Circle(RANDOM.nextInt(Constants.Size.WIDTH), RANDOM.nextInt(Constants.Size.HEIGHT), 1);
			star.setFill(Color.WHITE);
			star.setOpacity(0.3 + RANDOM.nextDouble() * 0.7);
			gameLayer.getChildren().add(star);
		}
	}

	private VBox shipDestroyedMenu(Pane overlayLayer, Pane gameLayer) {
		Text shipDestroyedText = new Text();
		if (lives > 0) {
			shipDestroyedText.setText("VESSEL CRASHED");
			shipDestroyedText.setStroke(Color.ORANGE);
		} else {
			shipDestroyedText.setText("GAME OVER");
			shipDestroyedText.setStroke(Color.RED);
		}
		shipDestroyedText.getStyleClass().add("dialog-title");

		GridPane statsGrid = new GridPane();
		statsGrid.setHgap(10);
		statsGrid.setVgap(10);
		statsGrid.setAlignment(Pos.CENTER);

		addStatRow(statsGrid, 0, "Asteroids Shot", String.valueOf(points));

		addStatRow(statsGrid, 1, "Score", String.valueOf(score));

		addStatRow(statsGrid, 2, "Lives", String.valueOf(lives));

		addStatRow(statsGrid, 3, "Level", String.valueOf(level));

		addStatRow(statsGrid, 4, "Highscore", String.valueOf(highScore));

		for (Node node : statsGrid.getChildren()) {
			if (node instanceof Text text) {
				text.getStyleClass().add("dialog-stats");
			}
		}

		shipDestroyedBox = new VBox(10);

		Button respawnButton = new Button("Respawn");
		respawnButton.setPrefSize(140, 45);
		respawnButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: limegreen; -fx-border-color: green; -fx-border-width: 2px; -fx-font-family: 'Consolas';");

		respawnButton.setOnAction(event -> {
			respawnShip();
			gameLayer.getChildren().remove(shipDestroyedBox);
			SoundPlayer.play();
			paused = false;
		});

		Button restartButton = new Button("Restart");
		restartButton.setPrefSize(140, 45);
		restartButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: limegreen; -fx-border-color: green; -fx-border-width: 2px; -fx-font-family: 'Consolas';");

		restartButton.setOnAction(event -> {
			resetGame(gameLayer);
		});

		shipDestroyedBox.getChildren().addAll(shipDestroyedText, statsGrid, restartButton);
		if (lives > 0) {
			shipDestroyedBox.getChildren().addAll(respawnButton);
		}
		shipDestroyedBox.setAlignment(Pos.CENTER);
		shipDestroyedBox.getStyleClass().add("overlay-box");

		return shipDestroyedBox;
	}

	private VBox pausedMenu() {
		VBox pausedMenu = new VBox();

		Text pauseText = new Text("Pilot on a Break");
		pauseText.getStyleClass().add("pause-title");

		Text pauseMsg = new Text("Press ESC to resume");
		pauseMsg.getStyleClass().add("pause-message");

		pausedMenu.getChildren().addAll(pauseText, pauseMsg);
		pausedMenu.setAlignment(Pos.CENTER);
		pausedMenu.getStyleClass().add("overlay-box");
		pausedMenu.setVisible(false);

		return pausedMenu;
	}

	private int loadHighScore() {
		try {
			return Integer.parseInt(Files.readString(Path.of("highscore.txt")));
		} catch (Exception e) {
			return 0;
		}
	}

	private void saveHighScore(int highScore) {
		try {
			Files.writeString(Path.of("highscore.txt"), String.valueOf(highScore));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void respawnShip() {

		ship.getShape().setTranslateX(Constants.Size.WIDTH / 2);

		ship.getShape().setTranslateY(Constants.Size.HEIGHT / 2);

		ship.getShape().setRotate(0);

		ship.setMovement(new Point2D(0.5, 0));

		invulnerabilityFrames = Constants.Value.INVULNERABILITY_FRAMES;

	}

	private void resetGame(Pane gameLayer) {
		gameLayer.getChildren().remove(shipDestroyedBox);

		projectiles.forEach(p -> gameLayer.getChildren().remove(p.getShape()));
		projectiles.clear();

		asteroids.forEach(a -> gameLayer.getChildren().remove(a.getShape()));
		asteroids.clear();

		resetStats();

		createInitialAsteroids(gameLayer);

		respawnShip();

		updateHUD();

		paused = false;

		showLevelUp();
		SoundPlayer.play();

	}

	private void createLevelUp(StackPane effectsLayer) {
		levelUpText.getStyleClass().add("level-up-text");
		levelUpText.setOpacity(0);

		StackPane.setAlignment(levelUpText, Pos.CENTER);

		effectsLayer.getChildren().add(levelUpText);
	}

	private void showLevelUp() {

		levelUpText.setText("LEVEL " + level);

		FadeTransition fade = new FadeTransition(Duration.seconds(2), levelUpText);
		fade.setFromValue(0.8);
		fade.setToValue(0.0);
		fade.play();
	}

	private void createHUD(Pane hudLayer) {

		updateHUD();

		Text title = new Text("ASTEROID DESTROYER");

		livesText.getStyleClass().add("hud-text");
		levelText.getStyleClass().add("hud-text");
		scoreText.getStyleClass().add("hud-text");
		highText.getStyleClass().add("hud-text");
		pointsText.getStyleClass().add("hud-text");

		title.getStyleClass().add("game-title");

		livesText.setTranslateX(15);
		livesText.setTranslateY(25);

		levelText.setTranslateX(Constants.Size.WIDTH - 95);
		levelText.setTranslateY(25);

		scoreText.setTranslateX(15);
		scoreText.setTranslateY(Constants.Size.HEIGHT - 15);

		highText.setTranslateX(Constants.Size.WIDTH - 110);
		highText.setTranslateY(Constants.Size.HEIGHT - 15);

		pointsText.setTranslateX(Constants.Size.WIDTH / 2 - 95);
		pointsText.setTranslateY(Constants.Size.HEIGHT - 15);

		title.setTranslateX(Constants.Size.WIDTH / 2 - 110);
		title.setTranslateY(25);

		hudLayer.getChildren().addAll(livesText, levelText, scoreText, highText, pointsText, title);
	}

	private void updateHUD() {
		scoreText.setText("Score: " + score);
		pointsText.setText(String.format("Asteroids Shot: %02d", points));
		livesText.setText("Lives: " + lives);
		highText.setText(String.format("High: %04d", highScore));
		levelText.setText(String.format("Level: %02d", level));
	}

	private void shootProjectile(Pane gameLayer) {
		Projectile projectile = new Projectile((int) ship.getShape().getTranslateX(),
				(int) ship.getShape().getTranslateY());
		projectile.getShape().setRotate(ship.getShape().getRotate());
		projectiles.add(projectile);

		SoundPlayer.playShoot();

		projectile.accelerate();
		projectile.setMovement(projectile.getMovement().normalize().multiply(Constants.Value.PROJECTILE_SPEED));

		gameLayer.getChildren().add(projectile.getShape());
	}

	private void asteroidDestroyed() {
		points++;
		score += Constants.Value.SCORE_PER_ASTEROID;

		int previousLevel = level;

		level = (points / 10) + 1;

		if (level > previousLevel) {
			SoundPlayer.levelUp();
			showLevelUp();
		}

		if (score > highScore) {
			highScore = score;
			saveHighScore(highScore);
		}

		updateHUD();
	}

	private void resetStats() {
		points = 0;
		score = 0;
		lives = 3;
		level = 1;
	}

	private void addStatRow(GridPane grid, int row, String label, String value) {
		grid.add(new Text(label), 0, row);
		grid.add(new Text(":"), 1, row);
		grid.add(new Text(value), 2, row);
	}
}
