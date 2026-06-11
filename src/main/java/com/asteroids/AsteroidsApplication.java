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
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class AsteroidsApplication extends Application {

	private static final Random RANDOM = new Random();
	private int points = 0;
	private boolean paused = false;
	private int score = 0;
	private int highScore = loadHighScore();
	private int lives = 3;
	private Ship ship;
	private int invulnerabilityFrames = 120;
	private List<Asteroid> asteroids;
	private List<Projectile> projectiles;
	private Pane pane;
	private VBox shipDestroyedBox;
	private Text textS;
	private Text textP;
	private Text textL;
	private Text textHS;
	private int level = 1;

	@Override
	public void start(Stage stage) throws Exception {

		BorderPane mainScreen = new BorderPane();

		pane = new Pane();
		pane.setPrefSize(Constants.Size.WIDTH, Constants.Size.HEIGHT);
		pane.setStyle("-fx-background-color: black;");

		Rectangle clip = new Rectangle(Constants.Size.WIDTH, Constants.Size.HEIGHT);

		pane.setClip(clip);

		textS = new Text("Score:" + score);
		textP = new Text("Asteroids Shot:" + points);
		textL = new Text("Lives:" + lives);
		textHS = new Text("High Score:" + highScore);

		GridPane hud = new GridPane();
		hud.setHgap(20);

		hud.add(textS, 0, 0);
		hud.add(textP, 1, 0);
		hud.add(textL, 2, 0);
		hud.add(textHS, 3, 0);

		for (Node node : hud.getChildren()) {
			if (node instanceof Text) {
				((Text) node).setStyle("-fx-font-size: 17px;" + "-fx-fill: blue;" + "-fx-font-family: 'Consolas';");
			}
		}

		HBox hbox = new HBox();
		hbox.getChildren().addAll(hud);
		hbox.setAlignment(Pos.CENTER);
		hbox.setStyle("-fx-background-color: limegreen;");

		mainScreen.setTop(hbox);

		createStarField(pane);

		ship = new Ship(Constants.Size.WIDTH / 2, Constants.Size.HEIGHT / 2);
		asteroids = new ArrayList<>();
		projectiles = new ArrayList<>();

		pane.getChildren().add(ship.getShape());

		createInitialAsteroids(asteroids);

		Map<KeyCode, Boolean> pressedKeys = new HashMap<>();

		mainScreen.setCenter(pane);

		Scene scene = new Scene(mainScreen);

		VBox pauseBox = pausedMenu(scene);

		pane.getChildren().add(pauseBox);

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

					pane.getChildren().add(projectile.getShape());
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
						pane.getChildren().add(shipDestroyed(stage, pane));
					} else {
						SoundPlayer.playCrash();
						pane.getChildren().add(shipDestroyed(stage, pane));
					}
				}

				handleProjectileCollisions(projectiles, asteroids, textS, textP, textL, textHS);

				removeDestroyedProjectiles(projectiles, pane);

				removeDestroyedAsteroids(asteroids, pane);

				spawnAsteroid(ship, asteroids, pane);

			}
		}.start();

		SoundPlayer.play();

		stage.setTitle("Asteroids!");
		stage.setScene(scene);
		stage.show();
	}

	private void createInitialAsteroids(List<Asteroid> asteroids) {
		for (int i = 0; i < Constants.Value.INITIAL_ASTEROIDS; i++) {
			Asteroid a = new Asteroid(RANDOM.nextInt(Constants.Size.WIDTH), RANDOM.nextInt(Constants.Size.HEIGHT));
			asteroids.add(a);
			pane.getChildren().add(a.getShape());
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
			Text pointsText, Text livesText, Text highScoreText) {

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

					if (score > highScore) {
						highScore = score;
						saveHighScore(highScore);
					}

					scoreText.setText("Score:" + score);
					pointsText.setText("Asteroids Shot:" + points);
					livesText.setText("Lives:" + lives);
					highScoreText.setText("High Score:" + highScore);
				}
			});
		});
	}

	private void removeDestroyedProjectiles(List<Projectile> projectiles, Pane pane) {

		projectiles.stream().filter(projectile -> !projectile.isAlive())
				.forEach(projectile -> pane.getChildren().remove(projectile.getShape()));

		projectiles.removeAll(
				projectiles.stream().filter(projectile -> !projectile.isAlive()).collect(Collectors.toList()));
	}

	private void removeDestroyedAsteroids(List<Asteroid> asteroids, Pane pane) {

		asteroids.stream().filter(asteroid -> !asteroid.isAlive())
				.forEach(asteroid -> pane.getChildren().remove(asteroid.getShape()));

		asteroids.removeAll(asteroids.stream().filter(asteroid -> !asteroid.isAlive()).collect(Collectors.toList()));
	}

	private void spawnAsteroid(Ship ship, List<Asteroid> asteroids, Pane pane) {

		double spawnChance = Constants.Value.ASTEROID_SPAWN_CHANCE + ((level - 1) * 0.002);
		if (RANDOM.nextDouble() < spawnChance) {

			Asteroid asteroid = new Asteroid(RANDOM.nextInt(Constants.Size.WIDTH),
					RANDOM.nextInt(Constants.Size.HEIGHT));

			if (!asteroid.collide(ship)) {

				asteroids.add(asteroid);
				pane.getChildren().add(asteroid.getShape());
			}
		}
	}

	private void createStarField(Pane pane) {
		for (int i = 0; i < 100; i++) {
			Circle star = new Circle(RANDOM.nextInt(Constants.Size.WIDTH), RANDOM.nextInt(Constants.Size.HEIGHT), 1);
			star.setFill(Color.WHITE);
			star.setOpacity(0.3 + RANDOM.nextDouble() * 0.7);
			pane.getChildren().add(star);
		}
	}

	private VBox shipDestroyed(Stage stage, Pane pane) {
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

		statsGrid.add(new Text("High Score"), 0, 3);
		statsGrid.add(new Text(":"), 1, 3);
		statsGrid.add(new Text(String.valueOf(highScore)), 2, 3);

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
			pane.getChildren().remove(shipDestroyedBox);
			invulnerabilityFrames = 120;
			SoundPlayer.play();
			paused = false;
		});

		Button restartButton = new Button("Restart");
		restartButton.setPrefSize(140, 45);
		restartButton.setStyle(
				"-fx-font-size: 16px; -fx-background-color: limegreen; -fx-border-color: green; -fx-border-width: 2px; -fx-font-family: 'Consolas';");

		restartButton.setOnAction(event -> {
			resetGame();
		});

		shipDestroyedBox.getChildren().addAll(shipDestroyedText, statsGrid, restartButton);
		if (lives > 0) {
			shipDestroyedBox.getChildren().addAll(respawnButton);
		}
		shipDestroyedBox.setAlignment(Pos.CENTER);
		shipDestroyedBox.setStyle(
				"-fx-background-color: rgba(30,30,30,0.85); -fx-padding: 25; -fx-border-color: white; -fx-border-width: 2;");
		shipDestroyedBox.layoutXProperty()
				.bind(stage.getScene().widthProperty().subtract(shipDestroyedBox.widthProperty()).divide(2));
		shipDestroyedBox.layoutYProperty().bind(
				stage.getScene().heightProperty().subtract(shipDestroyedBox.heightProperty()).divide(2).subtract(10));

		return shipDestroyedBox;
	}

	private VBox pausedMenu(Scene scene) {
		VBox pausedMenu = new VBox();

		Text pauseText = new Text("Pilot on a Break");
		pauseText.setStyle("-fx-font-size: 40px; -fx-fill: yellow; -fx-font-family: 'Consolas';");

		Text pauseMsg = new Text("Press ESC to resume");
		pauseMsg.setStyle("-fx-font-size: 20px; -fx-fill: orange; -fx-font-family: 'Consolas';");

		pausedMenu.getChildren().addAll(pauseText, pauseMsg);
		pausedMenu.setAlignment(Pos.CENTER);
		pausedMenu.setStyle(
				"-fx-background-color: rgba(30,30,30,0.85); -fx-padding: 25; -fx-border-color: white; -fx-border-width: 2;");
		pausedMenu.layoutXProperty().bind(scene.widthProperty().subtract(pausedMenu.widthProperty()).divide(2));
		pausedMenu.layoutYProperty()
				.bind(scene.heightProperty().subtract(pausedMenu.heightProperty()).divide(2).subtract(10));
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

	}

	private void resetGame() {
		pane.getChildren().remove(shipDestroyedBox);

		projectiles.forEach(p -> pane.getChildren().remove(p.getShape()));
		projectiles.clear();

		asteroids.forEach(a -> pane.getChildren().remove(a.getShape()));
		asteroids.clear();

		createInitialAsteroids(asteroids);

		points = 0;
		score = 0;
		lives = 3;
		level = 1;

		textS.setText("Score:" + score);
		textP.setText("Asteroids Shot:" + points);
		textL.setText("Lives:" + lives);

		respawnShip();
		paused = false;
		SoundPlayer.play();

	}

}
