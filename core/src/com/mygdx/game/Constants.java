package com.mygdx.game;

public class Constants {

    public static final int MAX_FPS = 60;
    public static final float TIME_STEP = 1 / 1200f; // Target physics time step
    public static final int PHYSICS_STEPS_PER_FRAME = 4; // Number of physics updates per frame
    public static final int VELOCITY_ITERATIONS = 6;
    public static final int POSITION_ITERATIONS = 4;
    public static final float PPM = 100; // Pixels per Meter
    public static final float WORLD_WIDTH = 8; // 800 pixels in width
    public static final float WORLD_HEIGHT = 6; // 600 pixels in height
    public static final float BALL_RADIUS_MIN = 0.1f; // Min radius in meters
    public static final float BALL_RADIUS_RANGE = 0.5f; // Range for random additional radius
    public static final float CIRCLE_RADIUS = 5f; // Circle radius in meters
    public static final float SECONDS_OPEN = Integer.MAX_VALUE;
    public static final float BALL_DEFAULT_DENSITY = 0.5f;
    public static final float BALL_DEFAULT_FRICTION = 0.1f;
    public static final float BALL_DEFAULT_RESTITUTION = 1.5f;
}


