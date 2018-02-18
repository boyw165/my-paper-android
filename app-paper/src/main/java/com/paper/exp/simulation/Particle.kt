package com.paper.exp.simulation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * The `Particle` class represents a particle moving in the unit box,
 * with a given position, velocity, radius, and mass. Methods are provided
 * for moving the particle and for predicting and resolvling elastic
 * collisions with vertical walls, horizontal walls, and other particles.
 * This data type is mutable because the position and velocity change.
 *
 *
 * For additional documentation,
 * see [Section 6.1](https://algs4.cs.princeton.edu/61event) of
 * *Algorithms, 4th Edition* by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
class Particle constructor(private var mCenterX: Double = Math.random(),
                           private var mCenterY: Double = Math.random(),
                           private var mVecX: Double = 1f * ((100f * Math.random() - 50f) / 100f),
                           private var mVecY: Double = 1f * ((100f * Math.random() - 50f) / 100f),
                           private val mRadius: Double = 1f * 0.01,
                           private val mMass: Double = 1f * 0.5,
                           private val mColor: Int = Color.BLACK) {
    /**
     * Returns the number of collisions involving this particle with
     * vertical walls, horizontal walls, or other particles.
     * This is equal to the number of calls to [.bounceOff],
     * [.bounceOffVerticalWall], and
     * [.bounceOffHorizontalWall].
     *
     * @return the number of collisions involving this particle with
     * vertical walls, horizontal walls, or other particles
     */
    var collisionCount: Int = 0
        private set

    // For canvas rendering.
    private val mOval = RectF()

    val centerX: Double get() = mCenterX
    val centerY: Double get() = mCenterY
    val vecX: Double get() = mVecX
    val vecY: Double get() = mVecY

    /**
     * Moves this particle in a straight line (based on its velocity)
     * for the specified amount of time.
     *
     * @param dt the amount of time
     */
    fun move(dt: Double) {
        mCenterX += mVecX * dt
        mCenterY += mVecY * dt
    }

    /**
     * Draws this particle to standard draw.
     */
    fun draw(canvas: Canvas,
             canvasWidth: Int,
             canvasHeight: Int,
             paint: Paint) {
        paint.color = mColor

        mOval.set((mCenterX - mRadius).toFloat() * canvasWidth,
                  (mCenterY - mRadius).toFloat() * canvasHeight,
                  (mCenterX + mRadius).toFloat() * canvasWidth,
                  (mCenterY + mRadius).toFloat() * canvasHeight)
        canvas.drawOval(mOval,
                        paint)
    }

    /**
     * Returns the amount of time for this particle to collide with the specified
     * particle, assuming no interening collisions.
     *
     * @param that the other particle
     * @return the amount of time for this particle to collide with the specified
     * particle, assuming no interening collisions;
     * `Double.POSITIVE_INFINITY` if the particles will not collide
     */
    fun timeToHit(that: Particle): Double {
        if (this === that) return INFINITY

        val dx = that.mCenterX - this.mCenterX
        val dy = that.mCenterY - this.mCenterY
        val dvx = that.mVecX - this.mVecX
        val dvy = that.mVecY - this.mVecY
        val dvdr = dx * dvx + dy * dvy
        if (dvdr >= 0) return INFINITY

        val dvdv = dvx * dvx + dvy * dvy
        val drdr = dx * dx + dy * dy
        val sigma = this.mRadius + that.mRadius
        val d = dvdr * dvdr - dvdv * (drdr - sigma * sigma)
        // if (drdr < sigma*sigma) StdOut.println("overlapping particles");
        return if (d < 0) INFINITY else -(dvdr + Math.sqrt(d)) / dvdv
    }

    /**
     * Returns the amount of time for this particle to collide with a vertical
     * wall, assuming no interening collisions.
     *
     * @return the amount of time for this particle to collide with a vertical wall,
     * assuming no interening collisions;
     * `Double.POSITIVE_INFINITY` if the particle will not collide
     * with a vertical wall
     */
    fun timeToHitVerticalWall(): Double {
        return if (mVecX > 0) {
            (1.0 - mCenterX - mRadius) / mVecX
        } else if (mVecX < 0) {
            (mRadius - mCenterX) / mVecX
        } else {
            INFINITY
        }
    }

    /**
     * Returns the amount of time for this particle to collide with a horizontal
     * wall, assuming no interening collisions.
     *
     * @return the amount of time for this particle to collide with a horizontal wall,
     * assuming no interening collisions;
     * `Double.POSITIVE_INFINITY` if the particle will not collide
     * with a horizontal wall
     */
    fun timeToHitHorizontalWall(): Double {
        return if (mVecY > 0) {
            (1.0 - mCenterY - mRadius) / mVecY
        } else if (mVecY < 0) {
            (mRadius - mCenterY) / mVecY
        } else {
            INFINITY
        }
    }

    /**
     * Updates the velocities of this particle and the specified particle according
     * to the laws of elastic collision. Assumes that the particles are colliding
     * at this instant.
     *
     * @param that the other particle
     */
    fun bounceOff(that: Particle) {
        val dx = that.mCenterX - this.mCenterX
        val dy = that.mCenterY - this.mCenterY
        val dvx = that.mVecX - this.mVecX
        val dvy = that.mVecY - this.mVecY
        val dvdr = dx * dvx + dy * dvy             // dv dot dr
        val dist = this.mRadius + that.mRadius   // distance between particle centers at collison

        // magnitude of normal force
        val magnitude = 2.0 * this.mMass * that.mMass * dvdr / ((this.mMass + that.mMass) * dist)

        // normal force, and in x and y directions
        val fx = magnitude * dx / dist
        val fy = magnitude * dy / dist

        // update velocities according to normal force
        this.mVecX += fx / this.mMass
        this.mVecY += fy / this.mMass
        that.mVecX -= fx / that.mMass
        that.mVecY -= fy / that.mMass

        // update collision counts
        this.collisionCount++
        that.collisionCount++
    }

    /**
     * Updates the velocity of this particle upon collision with a vertical
     * wall (by reflecting the velocity in the *x*-direction).
     * Assumes that the particle is colliding with a vertical wall at this instant.
     */
    fun bounceOffVerticalWall() {
        mVecX = -mVecX
        collisionCount++
    }

    /**
     * Updates the velocity of this particle upon collision with a horizontal
     * wall (by reflecting the velocity in the *y*-direction).
     * Assumes that the particle is colliding with a horizontal wall at this instant.
     */
    fun bounceOffHorizontalWall() {
        mVecY = -mVecY
        collisionCount++
    }

    /**
     * Returns the kinetic energy of this particle.
     * The kinetic energy is given by the formula 1/2 *m* *v*<sup>2</sup>,
     * where *m* is the mass of this particle and *v* is its velocity.
     *
     * @return the kinetic energy of this particle
     */
    fun kineticEnergy(): Double {
        return 0.5 * mMass * (mVecX * mVecX + mVecY * mVecY)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    companion object {
        private val INFINITY = java.lang.Double.POSITIVE_INFINITY
    }
}
/**
 * Initializes a particle with a random position and velocity.
 * The position is uniform in the unit box; the velocity in
 * either direction is chosen uniformly at random.
 */
