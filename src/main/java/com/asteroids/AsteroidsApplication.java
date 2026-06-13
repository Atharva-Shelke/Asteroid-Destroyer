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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AsteroidsApplication extends Application {

	private static final Random RANDOM = new Random();
	private int points = 0;
	private boolean paused = false;
	private int score = 0;
	private int highScore = loadHighScore();
	private int lives = 3;
	private Ship ship;
	private int invulnerabilityFrames = Constants.Value.INVULNERABILITY_FRAMES;
	private List<Asteroid> asteroids;
	private List<Projectile> projectiles;
	private VBox shipDestroyedBox;
	private Text textS;
	private Text textP;
	private Text textL;
	private Text textHS;
	private int level = 1;
	private Text textLevel;
	private Text levelUpText;

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

		Rectangle clip = new Rectangle(Constants.Size.WIDTH, Constants.Size.HEIGHT);

		gameLayer.setClip(clip);

		textS = new Text("Score:" + score);
		textP = new Text("Asteroids Shot:" + points);
		textL = new Text("Lives:" + lives);
		textHS = new Text("High Score:" + highScore);
		textLevel = new Text("Level:" + level);

		GridPane hud = new GridPane();
		hud.setHgap(20);

		hud.add(textS, 0, 0);
		hud.add(textP, 1, 0);
		hud.add(textL, 2, 0);
		hud.add(textHS, 4, 0);
		hud.add(textLevel, 3, 0);

		for (Node node : hud.getChildren()) {
			if (node instanceof Text) {
				((Text) node).setStyle("-fx-font-size: 17px;" + "-fx-fill: blue;" + "-fx-font-family: 'Consolas';");
			}
		}

		HBox hbox = new HBox();
		hbox.getChildren().addAll(hud);
		hbox.setAlignment(Pos.CENTER);
		hbox.setStyle("-fx-background-color: limegreen;");

		hudLayer.getChildren().add(hbox);

		createStarField(gameLayer);

		createLevelUp(effectsLayer);

		ship = new Ship(Constants.Size.WIDTH / 2, Constants.Size.HEIGHT / 2);
		asteroids = new ArrayList<>();
		projectiles = new ArrayList<>();

		gameLayer.getChildren().add(ship.getShape());

		createInitialAsteroids(asteroids, gameLayer);

		Map<KeyCode, Boolean> pressedKeys = new HashMap<>();

		Scene scene = new Scene(gameArea);

		VBox pauseBox = pausedMenu();

		overlayLayer.getChildren().add(pauseBox);

		scene.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.SPACE) {
				if (projectiles.size() < Constants.Value.MAX_PROJECTILES) {
					// we shoot
					Projectile projectile = new Projectile((int) ship.getShape().getTranslateX(),
							(int) ship.getShape().getTranslateY());
					projectile.getShape().setRotate(ship.getShape().getRotate());
					projectiles.add(projectile);

					SoundPlayer.playShoot();

					projectile.accelerate();
					projectile.setMovement(
							projectile.getMovement().normalize().multiply(Constants.Value.PROJECTILE_SPEED));

					gameLayer.getChildren().add(projectile.getShape());
				}
			} else if (event.getCode() == KeyCode.ESCAPE) {

				paused = !paused;
				pauseBox.setVisible(paused);
				pauseBox.toFront();
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

				handleInput(ship, pressedKeys);

				moveObjects(ship, asteroids, projectiles);

				if (invulnerabilityFrames <= 0 && shipCollided(ship, asteroids)) {
					paused = true;
					lives--;
					textL.setText("Lives:" + lives);

					if (lives <= 0) {
						SoundPlayer.playGameOver();
						overlayLayer.getChildren().add(shipDestroyed(overlayLayer, gameLayer));
					} else {
						SoundPlayer.playCrash();
						overlayLayer.getChildren().add(shipDestroyed(overlayLayer, gameLayer));
					}
				}

				handleProjectileCollisions(projectiles, asteroids, textS, textP, textL, textHS, textLevel);

				removeDestroyedProjectiles(projectiles, gameLayer);

				removeDestroyedAsteroids(asteroids, gameLayer);

				spawnAsteroid(ship, asteroids, gameLayer);

			}
		}.start();

		SoundPlayer.play();

		stage.setTitle("Asteroids!");
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	private void createInitialAsteroids(List<Asteroid> asteroids, Pane gameLayer) {
		for (int i = 0; i < Constants.Value.INITIAL_ASTEROIDS; i++) {
			Asteroid a = new Asteroid(RANDOM.nextInt(Constants.Size.WIDTH), RANDOM.nextInt(Constants.Size.HEIGHT));
			asteroids.add(a);
			gameLayer.getChildren().add(a.getShape());
		}
	}

	private void handleInput(Ship ship, Map<KeyCode, Boolean> pressedKeys) {

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

	private void moveObjects(Ship ship, List<Asteroid> asteroids, List<Projectile> projectiles) {

		ship.move();
		asteroids.forEach(Asteroid::move);
		projectiles.forEach(Projectile::move);
	}

	private boolean shipCollided(Ship ship, List<Asteroid> asteroids) {

		return asteroids.stream().anyMatch(ship::collide);
	}

	private void handleProjectileCollisions(List<Projectile> projectiles, List<Asteroid> asteroids, Text scoreText,
			Text pointsText, Text livesText, Text highScoreText, Text textLevel) {

		projectiles.forEach(projectile -> {
			asteroids.forEach(asteroid -> {

				if (projectile.collide(asteroid)) {

					SoundPlayer.playExplosion();

					projectile.setAlive(false);
					asteroid.setAlive(false);
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

					scoreText.setText("Score:" + score);
					pointsText.setText("Asteroids Shot:" + points);
					livesText.setText("Lives:" + lives);
					highScoreText.setText("High Score:" + highScore);
					textLevel.setText("Level:" + level);
				}
			});
		});
	}

	private void removeDestroyedProjectiles(List<Projectile> projectiles, Pane gameLayer) {

		projectiles.stream().filter(projectile -> !projectile.isAlive())
				.forEach(projectile -> gameLayer.getChildren().remove(projectile.getShape()));

		projectiles.removeAll(
				projectiles.stream().filter(projectile -> !projectile.isAlive()).collect(Collectors.toList()));
	}

	private void removeDestroyedAsteroids(List<Asteroid> asteroids, Pane gameLayer) {

		asteroids.stream().filter(asteroid -> !asteroid.isAlive())
				.forEach(asteroid -> gameLayer.getChildren().remove(asteroid.getShape()));

		asteroids.removeAll(asteroids.stream().filter(asteroid -> !asteroid.isAlive()).collect(Collectors.toList()));
	}

	private void spawnAsteroid(Ship ship, List<Asteroid> asteroids, Pane gameLayer) {

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

	private VBox shipDestroyed(Pane overlayLayer, Pane gameLayer) {
		Text shipDestroyedText = new Text("SHIP DESTROYED!");
		shipDestroyedText.setStyle("-fx-font-size: 40px; -fx-fill: red;");

		GridPane statsGrid = new GridPane();
		statsGrid.setHgap(10);
		statsGrid.setVgap(10);
		statsGrid.setAlignment(Pos.CENTER);

		statsGrid.add(new Text("Asteroids Shot"), 0, 0);
		statsGrid.add(new Text(":"), 1, 0);
		statsGrid.add(new Text(String.valueOf(points)), 2, 0);

		statsGrid.add(new Text("Score"), 0, 1);
		statsGrid.add(new Text(":"), 1, 1);
		statsGrid.add(new Text(String.valueOf(score)), 2, 1);

		statsGrid.add(new Text("Lives"), 0, 2);
		statsGrid.add(new Text(":"), 1, 2);
		statsGrid.add(new Text(String.valueOf(lives)), 2, 2);

		statsGrid.add(new Text("High Score"), 0, 4);
		statsGrid.add(new Text(":"), 1, 4);
		statsGrid.add(new Text(String.valueOf(highScore)), 2, 4);

		statsGrid.add(new Text("Level"), 0, 3);
		statsGrid.add(new Text(":"), 1, 3);
		statsGrid.add(new Text(String.valueOf(level)), 2, 3);

		for (Node node : statsGrid.getChildren()) {
			if (node instanceof Text) {
				((Text) node).setStyle("-fx-font-size: 28px;" + "-fx-fill: white;" + "-fx-font-family: 'Consolas';");
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
		shipDestroyedBox.setStyle(
				"-fx-background-color: rgba(30,30,30,0.85); -fx-padding: 25; -fx-border-color: white; -fx-border-width: 2;");

		return shipDestroyedBox;
	}

	private VBox pausedMenu() {
		VBox pausedMenu = new VBox();

		Text pauseText = new Text("Pilot on a Break");
		pauseText.setStyle("-fx-font-size: 40px; -fx-fill: yellow; -fx-font-family: 'Consolas';");

		Text pauseMsg = new Text("Press ESC to resume");
		pauseMsg.setStyle("-fx-font-size: 20px; -fx-fill: orange; -fx-font-family: 'Consolas';");

		pausedMenu.getChildren().addAll(pauseText, pauseMsg);
		pausedMenu.setAlignment(Pos.CENTER);
		pausedMenu.setStyle(
				"-fx-background-color: rgba(30,30,30,0.85); -fx-padding: 25; -fx-border-color: white; -fx-border-width: 2;");
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

		createInitialAsteroids(asteroids, gameLayer);

		points = 0;
		score = 0;
		lives = 3;
		level = 1;

		textS.setText("Score:" + score);
		textP.setText("Asteroids Shot:" + points);
		textL.setText("Lives:" + lives);
		textLevel.setText("Level:" + level);

		respawnShip();
		paused = false;
		SoundPlayer.play();

	}

	private void createLevelUp(StackPane effectsLayer) {
		levelUpText = new Text();
		levelUpText.setFill(Color.GOLD);
		levelUpText.setStyle("-fx-font-size: 72px;" + "-fx-font-weight: bold;");

		levelUpText.setStroke(Color.ORANGE);
		levelUpText.setStrokeWidth(3);
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
}
