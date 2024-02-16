package com.mygdx.game;

public class Constants {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final int MAX_FPS = 60;
    public static final float TIME_STEP = 1f / 60f; // Target physics time step
    public static final int PHYSICS_STEPS_PER_FRAME = 4; // Number of physics updates per frame
    public static final int VELOCITY_ITERATIONS = 6;
    public static final int POSITION_ITERATIONS = 4;
    public static final float PPM = 100; // Pixels per meter
    public static final float WORLD_WIDTH = WIDTH / PPM; // World width in meters
    public static final float WORLD_HEIGHT = HEIGHT / PPM; // World height in meters;
    public static final float BALL_DENSITY = 0.5f;
    public static final float BALL_FRICTION = 0.0f;
    public static final float BALL_RESTITUTION = 1.1f;
    public static final float BALL_RADIUS = 0.1f;

}
