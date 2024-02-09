package com.mygdx.game;

public class Constants {

    public static final int MAX_FPS = 0;
    public static final float TIME_STEP = 1 / 1000f; // Target physics time step
    public static final int PHYSICS_STEPS_PER_FRAME = 4; // Number of physics updates per frame
    public static final int VELOCITY_ITERATIONS = 6*4*2;
    public static final int POSITION_ITERATIONS = 2*4*2;
    public static final float PPM = 100; // Pixels per meter
    public static final float WORLD_WIDTH = 800 / PPM; // World width in meters
    public static final float WORLD_HEIGHT = 600 / PPM; // World height in meters;
}
