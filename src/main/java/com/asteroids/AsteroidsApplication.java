package com.asteroids;

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

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
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

	@Override
	public void start(Stage stage) throws Exception {
		BorderPane mainScreen = new BorderPane();

		Pane pane = new Pane();
		pane.setPrefSize(Constants.Size.WIDTH, Constants.Size.HEIGHT);
		pane.setStyle("-fx-background-color: black;");

		Rectangle clip = new Rectangle(Constants.Size.WIDTH, Constants.Size.HEIGHT);

		pane.setClip(clip);

		Text text = new Text("Asteroids Destroyed : 0");
		HBox hbox = new HBox(text);
		hbox.setAlignment(Pos.CENTER);
		hbox.setStyle("-fx-background-color: limegreen;");
		text.setStyle("-fx-fill: blue;-fx-font-size: 20px;");

		mainScreen.setTop(hbox);

		createStarField(pane);

		Ship ship = new Ship(Constants.Size.WIDTH / 2, Constants.Size.HEIGHT / 2);
		List<Asteroid> asteroids = new ArrayList<>();
		List<Projectile> projectiles = new ArrayList<>();

		createInitialAsteroids(asteroids);

		pane.getChildren().add(ship.getShape());
		asteroids.forEach(asteroid -> pane.getChildren().add(asteroid.getShape()));

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

					projectile.accelerate();
					projectile.setMovement(
							projectile.getMovement().normalize().multiply(Constants.Value.PROJECTILE_SPEED));

					pane.getChildren().add(projectile.getShape());
				}
			} else if (event.getCode() == KeyCode.ESCAPE) {

				paused = !paused;
				pauseBox.setVisible(paused);
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
				handleInput(ship, pressedKeys);

				moveObjects(ship, asteroids, projectiles);

				if (shipCollided(ship, asteroids)) {
					pane.getChildren().add(gameOver(stage));
					stop();
				}

				handleProjectileCollisions(projectiles, asteroids, text);

				removeDestroyedProjectiles(projectiles, pane);

				removeDestroyedAsteroids(asteroids, pane);

				spawnAsteroid(ship, asteroids, pane);

			}
		}.start();

		stage.setTitle("Asteroids!");
		stage.setScene(scene);
		stage.show();
	}

	private void createInitialAsteroids(List<Asteroid> asteroids) {
		for (int i = 0; i < Constants.Value.INITIAL_ASTEROIDS; i++) {
			Asteroid a = new Asteroid(RANDOM.nextInt(Constants.Size.WIDTH), RANDOM.nextInt(Constants.Size.HEIGHT));
			asteroids.add(a);
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

	private void handleProjectileCollisions(List<Projectile> projectiles, List<Asteroid> asteroids, Text scoreText) {

		projectiles.forEach(projectile -> {
			asteroids.forEach(asteroid -> {

				if (projectile.collide(asteroid)) {

					projectile.setAlive(false);
					asteroid.setAlive(false);
					points += Constants.Value.SCORE_PER_ASTEROID;

					scoreText.setText("Asteroids Destroyed : " + points);
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

		if (RANDOM.nextDouble() < Constants.Value.ASTEROID_SPAWN_CHANCE) {

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

	private VBox gameOver(Stage stage) {
		Text gameOverText = new Text("Game Over");
		gameOverText.setStyle("-fx-font-size: 40px;-fx-fill: red;");

		Text finalScore = new Text("Score : " + points * 10);
		finalScore.setStyle("-fx-font-size: 30px;-fx-fill: blue;");

		Button restartButton = new Button("Restart");
		restartButton.setPrefSize(140, 45);
		restartButton.setStyle("-fx-font-size: 16px;-fx-background-color: limegreen;");

		restartButton.setOnAction(event -> {
			try {
				points = 0;
				paused = false;
				start(stage);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		VBox gameOverBox = new VBox(10);
		gameOverBox.getChildren().addAll(gameOverText, finalScore, restartButton);
		gameOverBox.setAlignment(Pos.CENTER);
		gameOverBox.setStyle("-fx-background-color: gray;" + "-fx-padding: 20;" + "-fx-border-color: black;"
				+ "-fx-border-width: 1;");
		gameOverBox.layoutXProperty()
				.bind(stage.getScene().widthProperty().subtract(gameOverBox.widthProperty()).divide(2));
		gameOverBox.layoutYProperty()
				.bind(stage.getScene().heightProperty().subtract(gameOverBox.heightProperty()).divide(2));

		return gameOverBox;
	}

	private VBox pausedMenu(Scene stage) {
		VBox pausedMenu = new VBox();

		Text pauseText = new Text("PAUSED");
		pauseText.setStyle("-fx-font-size: 40px; -fx-fill: orange;");

		Text pauseMsg = new Text("Press ESC to resume");
		pauseMsg.setStyle("-fx-font-size: 20px; -fx-fill: darkblue;");

		pausedMenu.getChildren().addAll(pauseText, pauseMsg);
		pausedMenu.setAlignment(Pos.CENTER);
		pausedMenu.setStyle("-fx-background-color: lightblue;" + "-fx-padding: 20;" + "-fx-border-color: blue;"
				+ "-fx-border-width: 3;");
		pausedMenu.layoutXProperty().bind(stage.widthProperty().subtract(pausedMenu.widthProperty()).divide(2));
		pausedMenu.layoutYProperty().bind(stage.heightProperty().subtract(pausedMenu.heightProperty()).divide(2));
		pausedMenu.setVisible(false);

		return pausedMenu;
	}

}
