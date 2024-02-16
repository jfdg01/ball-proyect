package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.Iterator;

import static com.mygdx.game.Constants.*;

public class MyGdxGame extends Game {
    private GameScreen mainGameScreen;

    @Override
    public void create() {
        mainGameScreen = new GameScreen();
        this.setScreen(mainGameScreen);
    }
}

class GameScreen implements Screen {
    SpriteBatch spriteBatch;
    World world;
    Texture ballTexture;
    Box2DDebugRenderer debugRenderer;
    ShapeRenderer shapeRenderer;
    private float accumulator = 0;
    private float textAccumulator = Float.MAX_VALUE;
    private float timePassed = 0;
    private MyContactListener contactListener;
    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private Body circleBody;
    private Array<Body> ballBodies = new Array<>();
    private int numberOfBalls = 0;
    private BitmapFont font;
    private boolean isLKeyPressed = false;
    private float ballCreationCooldown = 0f;
    private final float ballCreationInterval = 0.2f;
    float speed = 0;
    int ballsCreated = 0;
    int ballsDestoyed = 0;
    int newBalls = 0;
    int oldBalls = 0;
    public void initializeCameras() {
        float viewportWidth = Gdx.graphics.getWidth() / PPM;
        float viewportHeight = Gdx.graphics.getHeight() / PPM;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        camera.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        camera.update();

        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        hudCamera.update();
    }

    /**
     * Creation functions
     */
    private void createBall() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody; // Set body type

        // Random position within the big circle
        float randAngle = (float) (Math.random() * 2 * Math.PI);
        float randRadius = (float) (Math.random() * CIRCLE_RADIUS);
        float randX = WORLD_WIDTH / 2 + (float) Math.cos(randAngle) * randRadius / 1.5f;
        float randY = WORLD_HEIGHT / 2 + (float) Math.sin(randAngle) * randRadius / 1.5f;
        bodyDef.position.set(randX, randY);

        Body ballBody = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        // Random radius within defined range
        float randBallRadius = BALL_RADIUS_MIN + (float) Math.random() * BALL_RADIUS_RANGE;
        circle.setRadius(randBallRadius); // Set radius in meters

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = BALL_DEFAULT_DENSITY;
        fixtureDef.friction = BALL_DEFAULT_FRICTION;
        fixtureDef.restitution = BALL_DEFAULT_RESTITUTION; // Bounciness

        ballBody.createFixture(fixtureDef);
        ballBody.setBullet(true); // High-speed movement
        ballBody.setUserData("Ball");

        ballBodies.add(ballBody);
        numberOfBalls++;
        ballsCreated++;

        circle.dispose(); // Always dispose of shapes after use
    }

    private void createCircle() {
        // Number of segments to approximate the circle
        int segments = 360; // Increase for a smoother circle

        // Calculate vertices for the circle's perimeter
        Vector2[] vertices = new Vector2[segments];
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            vertices[i] = new Vector2((float) Math.cos(angle) * CIRCLE_RADIUS, (float) Math.sin(angle) * CIRCLE_RADIUS);
        }

        // Create a chain shape and set it to form a loop
        ChainShape chainShape = new ChainShape();
        chainShape.createLoop(vertices);

        // Create a body definition
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        // Set the position of the body to the center of the world
        // Note: This sets the position of the body's origin; the chain will be centered around this point
        bodyDef.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2);

        // Create the body in the world
        circleBody = world.createBody(bodyDef);

        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = chainShape;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.2f;
        fixtureDef.restitution = 0.6f;

        // Create the fixture on the body
        circleBody.createFixture(fixtureDef);
        circleBody.setUserData("Circle");

        // Dispose of the chain shape
        chainShape.dispose();
    }

    private Mesh fullScreenQuad;

    public Mesh createFullScreenQuad() {
        float[] verts = new float[20];
        int i = 0;

        verts[i++] = -1; // x1
        verts[i++] = -1; // y1
        verts[i++] = 0;
        verts[i++] = 0f; // u1
        verts[i++] = 0f; // v1

        verts[i++] = 1f; // x2
        verts[i++] = -1; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u2
        verts[i++] = 0f; // v2

        verts[i++] = 1f; // x3
        verts[i++] = 1f; // y3
        verts[i++] = 0;
        verts[i++] = 1f; // u3
        verts[i++] = 1f; // v3

        verts[i++] = -1; // x4
        verts[i++] = 1f; // y4
        verts[i++] = 0;
        verts[i++] = 0f; // u4
        verts[i++] = 1f; // v4

        Mesh mesh = new Mesh( true, 4, 0,  // static mesh with 4 vertices and no indices
                new VertexAttribute( VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
                new VertexAttribute( VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE+"0" ) );

        mesh.setVertices( verts );
        return mesh;
    }

    public GameScreen() {

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(3);

        fullScreenQuad = createFullScreenQuad();

        ballTexture = new Texture(Gdx.files.internal("ball.png"));

        initializeCameras();

        Gdx.input.setInputProcessor(inputProcessor);

        spriteBatch = new SpriteBatch();
        world = new World(new Vector2(0, -10), true);
        world.setContactListener(contactListener = new MyContactListener(this));
        debugRenderer = new Box2DDebugRenderer();
        shapeRenderer = new ShapeRenderer();

        // Create first ball
        createBall();

        createCircle();
    }

    private void handleKeyboardAndBallCreation(float delta) {
        if (isLKeyPressed) {
            ballCreationCooldown -= delta;
            if (ballCreationCooldown <= 0) {
                createBall();
                ballCreationCooldown = ballCreationInterval;
            }
        }
    }

    private void stepWorld() {
        // Update the accumulator with the frame's delta time
        accumulator += Gdx.graphics.getDeltaTime();
        // Perform multiple physics steps per frame, if enough accumulated time
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;
        }
    }

    // After 20 seconds have passed in terms of delta, close the application
    private void closeApplication(float delta) {
        if (timePassed >= SECONDS_OPEN) {
            Gdx.app.exit();
        }
        timePassed += delta;
    }

    @Override
    public void render(float delta) {
        closeApplication(delta);
        stepWorld();
        handleKeyboardAndBallCreation(delta);

        int fastestBallIndex = getFastestBallIndex();

        camera.update();
        ScreenUtils.clear(0, 0, 0, 1);
        spriteBatch.setProjectionMatrix(hudCamera.combined);

        if (textAccumulator >= 0.1f) {
            textAccumulator = 0;
            speed = ballBodies.get(fastestBallIndex).getLinearVelocity().len();
        }

        spriteBatch.begin();
        font.draw(spriteBatch, "Fastest ball: " + String.format("%.2f", speed) + " m/s", 0, Gdx.graphics.getHeight());
        font.draw(spriteBatch, "Total balls: " + numberOfBalls, 0, Gdx.graphics.getHeight() - 50); // Adjust position as needed
        font.draw(spriteBatch, "Balls created: " + ballsCreated, 0, Gdx.graphics.getHeight() - 100); // Adjust position as needed
        font.draw(spriteBatch, "Balls destroyed: " + ballsDestoyed, 0, Gdx.graphics.getHeight() - 150); // Adjust position as needed
        spriteBatch.end();

        textAccumulator += delta;

        renderBallsWithSprites(fastestBallIndex);
        ballCreation();
        ballDestruction();
    }

    private int getFastestBallIndex() {
        int fastestBallIndex = 0;
        float maxSpeed = 0;
        for (int i = 0; i < ballBodies.size; i++) {
            float speed = ballBodies.get(i).getLinearVelocity().len();
            if (speed > maxSpeed) {
                maxSpeed = speed;
                fastestBallIndex = i;
            }
        }
        return fastestBallIndex;
    }

    private void renderBallsWithSprites(int fastestBallIndex) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderCircle();
        shapeRenderer.end();

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        for (int i = 0; i < ballBodies.size; i++) {
            Body ballBody = ballBodies.get(i);
            float x = ballBody.getPosition().x - ballBody.getFixtureList().first().getShape().getRadius();
            float y = ballBody.getPosition().y - ballBody.getFixtureList().first().getShape().getRadius();
            //if fastest change the color to green, else red using a ternary conditional operaor
            spriteBatch.setColor(i == fastestBallIndex ? Color.GREEN : Color.WHITE);

            spriteBatch.draw(ballTexture, x, y, ballBody.getFixtureList().first().getShape().getRadius() * 2, ballBody.getFixtureList().first().getShape().getRadius() * 2);
        }
        spriteBatch.end();
    }

    private void renderBallsWithShape(int fastestBallIndex) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Render the big circle
        renderCircle();

        // Render each ball
        for (int i = 0; i < ballBodies.size; i++) {
            renderBall(i, i == fastestBallIndex);
        }

        shapeRenderer.end();
    }

    private void renderCircle() {
        shapeRenderer.setColor(new Color(0.9f, 0.9f, 0.2f, 0.5f));
        shapeRenderer.circle(circleBody.getPosition().x, circleBody.getPosition().y, CIRCLE_RADIUS, 360);
    }

    private void renderBall(int index, boolean isFastest) {
        Body ballBody = ballBodies.get(index);
        shapeRenderer.setColor(isFastest ? new Color(0.9f, 0.2f, 0.2f, 1) : new Color(0.2f, 0.5f, 0.2f, 1));
        shapeRenderer.circle(ballBody.getPosition().x, ballBody.getPosition().y, ballBody.getFixtureList().first().getShape().getRadius(), 360);
    }

    private InputAdapter inputProcessor = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.L) {
                isLKeyPressed = true; // Set the flag when L key is pressed
                return true;
            } else if (keycode == Input.Keys.ESCAPE) {
                Gdx.app.exit();
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

    @Override
    public void dispose() {
        spriteBatch.dispose();
        world.dispose();
        debugRenderer.dispose();
        font.dispose();
        ballTexture.dispose();
        shapeRenderer.dispose();
        fullScreenQuad.dispose();
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width / PPM;
        camera.viewportHeight = height / PPM;
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
        camera.update();

        hudCamera.viewportWidth = width;
        hudCamera.viewportHeight = height;
        hudCamera.position.set(width / 2f, height / 2f, 0);
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

    public void handleBallCreation(int num) {
        newBalls += num;
    }

    public void handleBallDestruction(int num) {
        oldBalls += num;
    }

    public void ballCreation() {
        if (newBalls > 0) {
            newBalls--;
            createBall();
        }
    }

    public void ballDestruction() {
        if (ballBodies.isEmpty()) {
            return;
        }
        Iterator<Body> iterator = ballBodies.iterator();
        while (iterator.hasNext()) {
            Body ball = iterator.next();
            if (contactListener.getBallsToRemove().contains(ball, true)) {
                iterator.remove();
                world.destroyBody(ball);
            }
        }
        contactListener.getBallsToRemove().clear();
    }

    static class MyContactListener implements ContactListener {

        GameScreen gameScreen;
        private int ballsToCreate = 0;
        private final Array<Body> ballsToRemove;

        public MyContactListener(GameScreen gameScreen) {
            this.ballsToRemove = new Array<>();
            this.gameScreen = gameScreen;
        }

        public void beginContact(Contact contact) {
            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            Object fixtureAName = fixtureA.getBody().getUserData();
            Object fixtureBName = fixtureB.getBody().getUserData();

            // Ball-Circle Collision: Queue new ball creation
            if (("Ball".equals(fixtureAName) && "Circle".equals(fixtureBName)) || ("Circle".equals(fixtureAName) && "Ball".equals(fixtureBName))) {
                gameScreen.handleBallCreation(1);
            }

            // Ball-Ball Collision: Mark one of the balls for removal
            if ("Ball".equals(fixtureAName) && "Ball".equals(fixtureBName)) {
                ballsToRemove.add(fixtureA.getBody());
            }
        }

        @Override
        public void endContact(Contact contact) {

        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {
            // Called before the solver resolves a collision
        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {
            // Called after the solver has resolved a collision
        }

        public Array<Body> getBallsToRemove() {
            return ballsToRemove;
        }
    }
}
