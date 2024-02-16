package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import static com.mygdx.game.Constants.*;
import static com.mygdx.game.Constants.PPM;

public class MyGdxGame extends Game {
    private GameScreen mainGameScreen;

    @Override
    public void create() {
        mainGameScreen = new GameScreen();
        this.setScreen(mainGameScreen);
    }
}

class GameScreen implements Screen {
    SpriteBatch batch;
    private Array<Body> ballsToRemove = new Array<>();
    private boolean createBallFlag = false;

    World world;
    Box2DDebugRenderer debugRenderer;
    private float accumulator = 0;
    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;

    // private Body ballBody;
    private Array<Body> ballBodies = new Array<>();
    private int numberOfBalls = 0;
    private Body floorBody;
    private Body bottomWallBody;
    private Body topWallBody;

    public Body getCircleBody() {
        return circleBody;
    }

    private Body circleBody;
    private BitmapFont font;
    private boolean isLKeyPressed = false;
    private float ballCreationCooldown = 0f;
    private final float ballCreationInterval = 0.2f;

    public void initializeCameras() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT); // Use world size in meters
        camera.update();

        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.position.set(hudCamera.viewportWidth / 2, hudCamera.viewportHeight / 2, 0);
        hudCamera.update();
    }

    /**
     * Creation functions
     */
    private void createBall() {
        // First we create a body definition
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody; // Set body type
        float randX, randY;
        randX = (float) Math.random();
        randY = (float) Math.random();
        bodyDef.position.set(4 + randX, 3 + randY); // A central position within the visible world

        // Create the body in the world
        Body ballBody = world.createBody(bodyDef);

        // Create a circle shape and set its radius
        CircleShape circle = new CircleShape();
        float randRadius = (float) Math.random() / 7;
        circle.setRadius(BALL_RADIUS + randRadius);

        // Create a fixture definition using the shape
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = BALL_DENSITY;
        fixtureDef.friction = BALL_FRICTION;
        fixtureDef.restitution = BALL_RESTITUTION; // Make it bounce

        // Create the fixture on the body
        ballBody.createFixture(fixtureDef);
        ballBody.setBullet(true);

        ballBody.setUserData("ball");
        ballBodies.add(ballBody);
        numberOfBalls++;
        // this.ballBody = ballBodies.get(0);

        // Dispose of the shape
        circle.dispose();
    }

    public void createCircle() {
        float radius = 5f; // Semi-circle radius in meters
        int segments = 360; // Number of segments to approximate the semi-circle

        Vector2[] vertices = new Vector2[segments];

        // Calculate the vertices of the semi-circle
        for (int i = 0; i < segments; i++) {
            float angle = (float) i / (segments - 1) * (float) -Math.PI * 2;
            vertices[i] = new Vector2(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle));
        }

        // Create the chain shape for the semi-circle
        ChainShape chainShape = new ChainShape();
        chainShape.createChain(vertices);

        // Create a body definition for the static semi-circle
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(4, 3); // Position of the semi-circle's bottom center in the world

        // Create the static body in the world and assign it to the member variable
        circleBody = world.createBody(bodyDef);

        // Attach the chain shape to the body
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = chainShape;

        circleBody.createFixture(fixtureDef);

        // Remember to dispose of the shape
        chainShape.dispose();
        circleBody.setUserData("circle");
    }

    private void createFloor() {
        // Create our body definition
        BodyDef groundBodyDef = new BodyDef();
        // Set its world position
        groundBodyDef.position.set(new Vector2(3, 1));

        // Create a body from the definition and add it to the world
        floorBody = world.createBody(groundBodyDef); // Assign the created body to the floorBody variable

        // Create a polygon shape
        PolygonShape groundBox = new PolygonShape();
        // Set the polygon shape as a box spanning the width of the screen and 1 meter high
        // Remember, setAsBox takes half-width and half-height as arguments
        groundBox.setAsBox(camera.viewportWidth, 0.5f); // Half-width and half-height

        // Create a fixture from our polygon shape and add it to our ground body
        floorBody.createFixture(groundBox, 0.0f);

        // Clean up after ourselves
        groundBox.dispose();
    }

    private void createTunnelWalls() {
        float tunnelLength = 1000; // Significantly longer for the simulation
        float tunnelHeight = 4;
        float wallThickness = 0.5f;
        float initialAngle = degreesToRadians(45);

        // Adjust the position and dimensions as needed to cover the desired length
        bottomWallBody = createWall(new Vector2(0, -tunnelHeight / 2 - wallThickness / 2), tunnelLength, wallThickness, initialAngle);
        topWallBody = createWall(new Vector2(0, tunnelHeight / 2 + wallThickness / 2), tunnelLength, wallThickness, initialAngle);
    }

    private Body createWall(Vector2 position, float width, float height, float angle) {
        BodyDef wallBodyDef = new BodyDef();
        wallBodyDef.position.set(position);
        wallBodyDef.angle = angle; // Set the initial angle for the wall

        Body wallBody = world.createBody(wallBodyDef);

        PolygonShape wallBox = new PolygonShape();
        wallBox.setAsBox(width / 2, height / 2);

        wallBody.createFixture(wallBox, 0.0f);
        wallBox.dispose();

        return wallBody; // Return the created body
    }

    /**
     * Utility functions
     */
    private float degreesToRadians(float degrees) {
        return degrees * (float) Math.PI / 180;
    }

    private void tiltTunnel(boolean tiltRight) {
        float tiltDegrees = 5; // The tilt amount in degrees
        if (!tiltRight) {
            tiltDegrees = -tiltDegrees; // Tilt in the opposite direction
        }
        float tiltRadians = (float) Math.toRadians(tiltDegrees);

        // Apply the tilt to the floor, bottom wall, and top wall
        applyTilt(floorBody, tiltRadians);
        applyTilt(bottomWallBody, tiltRadians);
        applyTilt(topWallBody, tiltRadians);
    }

    private void tiltFloor(boolean tiltRight) {
        float tiltDegrees = 5; // The tilt amount in degrees
        if (!tiltRight) {
            tiltDegrees = -tiltDegrees; // Tilt in the opposite direction
        }
        float tiltRadians = (float) Math.toRadians(tiltDegrees);

        // Get the current angle of the floor
        float currentAngle = floorBody.getAngle();

        // Calculate the new angle
        float newAngle = currentAngle + tiltRadians;

        // Set the floor body's new angle
        floorBody.setTransform(floorBody.getPosition(), newAngle);
    }

    private void applyTilt(Body body, float tiltRadians) {
        if (body != null) { // Check if the body reference is initialized
            float currentAngle = body.getAngle();
            float newAngle = currentAngle + tiltRadians;
            body.setTransform(body.getPosition(), newAngle);
        }
    }

    private float calculateLerpFactor(float speed) {
        // Define minimum and maximum speeds at which you adjust the lerp factor
        final float minSpeed = 1f; // Adjust as needed
        final float maxSpeed = 10f; // Adjust as needed
        // Define the range for the lerp factor
        final float minLerp = 0.1f;
        final float maxLerp = 0.9f;

        // Normalize speed to a 0-1 range
        float normalizedSpeed = (speed - minSpeed) / (maxSpeed - minSpeed);
        normalizedSpeed = Math.max(0, Math.min(normalizedSpeed, 1)); // Clamp to 0-1

        // Linearly interpolate between minLerp and maxLerp based on normalizedSpeed
        return minLerp + (maxLerp - minLerp) * normalizedSpeed;
    }

    /*private void resetBallPosition() {
        if (!ballBody.isAwake()) ballBody.setAwake(true);
        ballBody.setTransform(4, 3, 0);
    }*/

    /*private void resetBall() {
        if (!ballBody.isAwake()) ballBody.setAwake(true);
        ballBody.setTransform(4, 3, 0); // Reset position and angle
        ballBody.setLinearVelocity(0, 0); // Reset velocity
        ballBody.setAngularVelocity(0); // Reset angular velocity
    }*/

    /**
     * Physics simulation functions
     */
    public GameScreen() {

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1);

        initializeCameras();

        Gdx.input.setInputProcessor(inputProcessor);

        batch = new SpriteBatch();
        world = new World(new Vector2(0, -10), true);
        debugRenderer = new Box2DDebugRenderer();

        world.setContactListener(new MyContactListener());

        createCircle();
        createBall();
    }

    @Override
    public void render(float delta) {
        Gdx.app.log("Debug", "Render start");

        // Update the accumulator with the frame's delta time
        accumulator += delta;

        if (!ballsToRemove.isEmpty()) {
            Gdx.app.log("Debug", "Removing balls");
            for (int i = 0; i < ballsToRemove.size; i++) {
                world.destroyBody(ballsToRemove.get(i));
                ballBodies.removeValue(ballsToRemove.get(i), true);
                numberOfBalls--;
            }
            ballsToRemove.clear();
        }

        if (createBallFlag) {
            Gdx.app.log("Debug", "Creating a ball");
            createBall();
            createBallFlag = false; // Reset the flag
        }

        // Perform multiple physics steps per frame, if enough accumulated time
        if (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;
        }


        // Calculate the lerp factor for smooth camera movement
        /*float lerpFactor = calculateLerpFactor(ballBody.getLinearVelocity().len());

        // Update camera position to follow the ball, using the calculated lerp factor
        Vector2 ballPosition = ballBody.getPosition();
        camera.position.x += (ballPosition.x - camera.position.x) * lerpFactor;
        camera.position.y += (ballPosition.y - camera.position.y) * lerpFactor;*/

        /*if (isLKeyPressed && ballCreationCooldown <= 0) {
            createBall();
            ballCreationCooldown = ballCreationInterval;
        }
        if (ballCreationCooldown > 0) {
            ballCreationCooldown -= delta;
        }*/

        // No ball in array goes faster than 200ms
        /*for (int i = 0; i < ballBodies.size; i++) {
            if (ballBodies.get(i).getLinearVelocity().len() > 200) {
                ballBodies.get(i).setLinearVelocity(ballBodies.get(i).getLinearVelocity().limit(200));
            }
        }*/

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Clear the screen and render the world
        ScreenUtils.clear(0, 0, 0, 1);
        debugRenderer.render(world, camera.combined);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        /*float speed = ballBody.getLinearVelocity().len();
        font.draw(batch, "Speed: " + String.format("%.2f", speed) + " m/s", (float) Gdx.graphics.getWidth() / 2 - 80, Gdx.graphics.getHeight() - 20);*/
        font.draw(batch, "Balls: " + numberOfBalls, (float) Gdx.graphics.getWidth() / 2 - 50, Gdx.graphics.getHeight() - 40); // Adjust position as needed
        batch.end();

        Gdx.app.log("Debug", "Render end");
    }

    /*private void doPhysicsStep(float deltaTime) {
        float frameTime = Math.min(deltaTime, MAX_STEP);
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;
        }
    }*/

    /*void changeProperties() {
        // Iterate through the Array<Fixture> returned by getFixtureList()
        for (int i = 0; i < ballBody.getFixtureList().size; i++) {
            // Change the restitution for each fixture
            ballBody.getFixtureList().get(i).setRestitution(0.5f);
            // You can also adjust other properties as needed
        }
    }*/

    private InputAdapter inputProcessor = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.P) {
                tiltTunnel(true); // Tilt one way
                // changeProperties();
                return true;
            } else if (keycode == Input.Keys.O) {
                tiltTunnel(false); // Tilt the other way
                return true;
            } else if (keycode == Input.Keys.K) {
                // resetBallPosition();
                return true;
            } else if (keycode == Input.Keys.L) {
                isLKeyPressed = true; // Set the flag when L key is pressed
                return true;
            }
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            if (keycode == Input.Keys.L) {
                isLKeyPressed = false; // Clear the flag when L key is released
                return true;
            }
            return false;
        }
    };

    // Implement other required methods of Screen interface (resize, show, hide, pause, resume, dispose)
    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        debugRenderer.dispose();
        font.dispose();
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
        // Update the game world camera's viewport size
        camera.viewportWidth = width / PPM;
        camera.viewportHeight = height / PPM;
        camera.update();

        // Update the HUD camera's viewport size
        hudCamera.viewportWidth = width;
        hudCamera.viewportHeight = height;
        hudCamera.position.set((float) width / 2, (float) height / 2, 0);
        hudCamera.update();
    }


    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    private class MyContactListener implements ContactListener {

        @Override
        public void beginContact(Contact contact) {
            Body bodyA = contact.getFixtureA().getBody();
            Body bodyB = contact.getFixtureB().getBody();

            // Check for collision between a ball and the circle
            if ((bodyA.getUserData() instanceof String && bodyA.getUserData().equals("circle") && bodyB.getUserData() instanceof String && bodyB.getUserData().equals("ball")) || (bodyB.getUserData() instanceof String && bodyB.getUserData().equals("circle") && bodyA.getUserData() instanceof String && bodyA.getUserData().equals("ball"))) {
                // Flag that a ball should be created
                createBallFlag = true;
                Gdx.app.log("Contact", "Ball collision");
            }
            // Check if both bodies are balls
            if ((bodyA.getUserData() instanceof String && bodyA.getUserData().equals("ball")) && (bodyB.getUserData() instanceof String && bodyB.getUserData().equals("ball"))) {
                // Add one of the balls to the removal list randomly
                if (Math.random() > 0.5) {
                    ballsToRemove.add(bodyA);
                } else {
                    ballsToRemove.add(bodyB);
                }
                Gdx.app.log("Contact", "Ball collision");
            }
        }


        @Override
        public void endContact(Contact contact) {
            // Handle end of contact if necessary
        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {
            // Optional: handle collision before it's resolved
        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {
            // Optional: handle results of collision, such as impact force
        }
    }

}