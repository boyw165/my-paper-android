package com.paper.exp.simulation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The {@code Particle} class represents a particle moving in the unit box,
 * with a given position, velocity, radius, and mass. Methods are provided
 * for moving the particle and for predicting and resolvling elastic
 * collisions with vertical walls, horizontal walls, and other particles.
 * This data type is mutable because the position and velocity change.
 * <p>
 * For additional documentation,
 * see <a href="https://algs4.cs.princeton.edu/61event">Section 6.1</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
public class Particle {
    private static final double INFINITY = Double.POSITIVE_INFINITY;

    private double mCenterX, mCenterY;
    private double mVecX, mVecY;
    private int mCollisionCount;
    private final double mRadius;
    private final double mMass;
    private final int mColor;

    // For canvas rendering.
    private final RectF mOval = new RectF();

    /**
     * Initializes a particle with the specified position, velocity, radius, mass, and color.
     *
     * @param rx     <em>x</em>-coordinate of position
     * @param ry     <em>y</em>-coordinate of position
     * @param vx     <em>x</em>-coordinate of velocity
     * @param vy     <em>y</em>-coordinate of velocity
     * @param radius the radius
     * @param mass   the mass
     * @param color  the color
     */
    public Particle(double rx,
                    double ry,
                    double vx,
                    double vy,
                    double radius,
                    double mass,
                    int color) {
        this.mVecX = vx;
        this.mVecY = vy;
        this.mCenterX = rx;
        this.mCenterY = ry;
        this.mRadius = radius;
        this.mMass = mass;
        this.mColor = color;
    }

    /**
     * Initializes a particle with a random position and velocity.
     * The position is uniform in the unit box; the velocity in
     * either direciton is chosen uniformly at random.
     */
    public Particle() {
        this(Math.random(),
             Math.random(),
             (1000f * Math.random() - 500f) / 1000f,
             (1000f * Math.random() - 500f) / 1000f,
             0.01,
             0.5,
             Color.BLACK);
    }

    /**
     * Moves this particle in a straight line (based on its velocity)
     * for the specified amount of time.
     *
     * @param dt the amount of time
     */
    public void move(double dt) {
        mCenterX += mVecX * dt;
        mCenterY += mVecY * dt;
    }

    /**
     * Draws this particle to standard draw.
     */
    public void draw(Canvas canvas,
                     int canvasWidth,
                     int canvasHeight,
                     Paint paint) {
        paint.setColor(mColor);

        mOval.set((float) (mCenterX - mRadius) * canvasWidth,
                  (float) (mCenterY - mRadius) * canvasHeight,
                  (float) (mCenterX + mRadius) * canvasWidth,
                  (float) (mCenterY + mRadius) * canvasHeight);
        canvas.drawOval(mOval,
                        paint);
    }

    /**
     * Returns the number of collisions involving this particle with
     * vertical walls, horizontal walls, or other particles.
     * This is equal to the number of calls to {@link #bounceOff},
     * {@link #bounceOffVerticalWall}, and
     * {@link #bounceOffHorizontalWall}.
     *
     * @return the number of collisions involving this particle with
     * vertical walls, horizontal walls, or other particles
     */
    public int getCollisionCount() {
        return mCollisionCount;
    }

    /**
     * Returns the amount of time for this particle to collide with the specified
     * particle, assuming no interening collisions.
     *
     * @param that the other particle
     * @return the amount of time for this particle to collide with the specified
     * particle, assuming no interening collisions;
     * {@code Double.POSITIVE_INFINITY} if the particles will not collide
     */
    public double timeToHit(Particle that) {
        if (this == that) return INFINITY;

        double dx = that.mCenterX - this.mCenterX;
        double dy = that.mCenterY - this.mCenterY;
        double dvx = that.mVecX - this.mVecX;
        double dvy = that.mVecY - this.mVecY;
        double dvdr = dx * dvx + dy * dvy;
        if (dvdr > 0) return INFINITY;

        double dvdv = dvx * dvx + dvy * dvy;
        double drdr = dx * dx + dy * dy;
        double sigma = this.mRadius + that.mRadius;
        double d = (dvdr * dvdr) - dvdv * (drdr - sigma * sigma);
        // if (drdr < sigma*sigma) StdOut.println("overlapping particles");
        if (d < 0) return INFINITY;

        return -(dvdr + Math.sqrt(d)) / dvdv;
    }

    /**
     * Returns the amount of time for this particle to collide with a vertical
     * wall, assuming no interening collisions.
     *
     * @return the amount of time for this particle to collide with a vertical wall,
     * assuming no interening collisions;
     * {@code Double.POSITIVE_INFINITY} if the particle will not collide
     * with a vertical wall
     */
    public double timeToHitVerticalWall() {
        if (mVecX > 0) {
            return (1.0 - mCenterX - mRadius) / mVecX;
        } else if (mVecX < 0) {
            return (mRadius - mCenterX) / mVecX;
        } else {
            return INFINITY;
        }
    }

    /**
     * Returns the amount of time for this particle to collide with a horizontal
     * wall, assuming no interening collisions.
     *
     * @return the amount of time for this particle to collide with a horizontal wall,
     * assuming no interening collisions;
     * {@code Double.POSITIVE_INFINITY} if the particle will not collide
     * with a horizontal wall
     */
    public double timeToHitHorizontalWall() {
        if (mVecY > 0) {
            return (1.0 - mCenterY - mRadius) / mVecY;
        } else if (mVecY < 0) {
            return (mRadius - mCenterY) / mVecY;
        } else {
            return INFINITY;
        }
    }

    /**
     * Updates the velocities of this particle and the specified particle according
     * to the laws of elastic collision. Assumes that the particles are colliding
     * at this instant.
     *
     * @param that the other particle
     */
    public void bounceOff(Particle that) {
        double dx = that.mCenterX - this.mCenterX;
        double dy = that.mCenterY - this.mCenterY;
        double dvx = that.mVecX - this.mVecX;
        double dvy = that.mVecY - this.mVecY;
        double dvdr = dx * dvx + dy * dvy;             // dv dot dr
        double dist = this.mRadius + that.mRadius;   // distance between particle centers at collison

        // magnitude of normal force
        double magnitude = 2 * this.mMass * that.mMass * dvdr / ((this.mMass + that.mMass) * dist);

        // normal force, and in x and y directions
        double fx = magnitude * dx / dist;
        double fy = magnitude * dy / dist;

        // update velocities according to normal force
        this.mVecX += fx / this.mMass;
        this.mVecY += fy / this.mMass;
        that.mVecX -= fx / that.mMass;
        that.mVecY -= fy / that.mMass;

        // update collision counts
        this.mCollisionCount++;
        that.mCollisionCount++;
    }

    /**
     * Updates the velocity of this particle upon collision with a vertical
     * wall (by reflecting the velocity in the <em>x</em>-direction).
     * Assumes that the particle is colliding with a vertical wall at this instant.
     */
    public void bounceOffVerticalWall() {
        mVecX = -mVecX;
        mCollisionCount++;
    }

    /**
     * Updates the velocity of this particle upon collision with a horizontal
     * wall (by reflecting the velocity in the <em>y</em>-direction).
     * Assumes that the particle is colliding with a horizontal wall at this instant.
     */
    public void bounceOffHorizontalWall() {
        mVecY = -mVecY;
        mCollisionCount++;
    }

    /**
     * Returns the kinetic energy of this particle.
     * The kinetic energy is given by the formula 1/2 <em>m</em> <em>v</em><sup>2</sup>,
     * where <em>m</em> is the mass of this particle and <em>v</em> is its velocity.
     *
     * @return the kinetic energy of this particle
     */
    public double kineticEnergy() {
        return 0.5 * mMass * (mVecX * mVecX + mVecY * mVecY);
    }
}
