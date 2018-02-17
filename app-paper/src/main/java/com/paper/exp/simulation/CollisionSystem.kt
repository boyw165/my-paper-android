package com.paper.exp.simulation

import android.graphics.Canvas
import android.graphics.Paint
import java.util.*

/**
 * The `CollisionSystem` class represents a collection of particles
 * moving in the unit box, according to the laws of elastic collision.
 * This event-based simulation relies on a priority queue.
 *
 *
 * For additional documentation,
 * see [Section 6.1](https://algs4.cs.princeton.edu/61event) of
 * *Algorithms, 4th Edition* by Robert Sedgewick and Kevin Wayne.
 *
 * Initializes a system with the specified collection of particles.
 * The individual particles will be mutated during the simulation.
 *
 * @param particles the array of particles
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
class CollisionSystem(particles: Array<Particle>) {
    private var mSimulationUpToMs: Long = 0L

    // the priority queue
    private val mPQ = PriorityQueue<Event>()
    // the array of particles
    private val mParticles: MutableList<Particle> = mutableListOf()

    // Clock.
    private var mClock: Long = 0L

    init {
        // defensive copy
        mParticles += particles
    }

    fun start() {
        // TODO: Make it an interface.
        // Hold clock.
        mClock = System.currentTimeMillis()

        mSimulationUpToMs = mClock + ONE_YEAR_MS

        // Rebuild the event queue.
        mPQ.clear()
        mParticles.forEachIndexed{i, particle ->
            predict(particle, mClock, mSimulationUpToMs)
        }

        // Check if any pending event is invalid.
        val event = mPQ.peek()
        if (event.time < mClock) {
            throw IllegalStateException(
                "The event time (%d) is even less than clock (%d)".format(event.time, mClock))
        }
    }

    /**
     * Simulates the system of particles for the specified amount of time.
     */
    fun simulate(canvas: Canvas,
                 canvasWidth: Int,
                 canvasHeight: Int,
                 particlePaint: Paint) {
        if (mPQ.isEmpty()) {
            start()
        } else {
            // TODO: The particle system shouldn't aware of the rendering details.

            // TODO: Make it an interface.
            var lastClock = mClock
            mClock = System.currentTimeMillis()

            // The main event-driven simulation loop
            while (!mPQ.isEmpty()) {
                // Get impending event, discard if invalidated
                val event = mPQ.remove()

                // Lazily discard the invalid events.
                if (!event.isValid) {
                    continue
                }

                if (event.time <= mClock) {
                    // Handle the particle-particle collision.
                    val a = event.a
                    val b = event.b

                    val dt = event.time - lastClock
                    if (dt < 0) {
//                        throw IllegalStateException("negative dt=%d".format(dt))
                        continue
                    }

                    // Physical collision, so update positions
                    lastClock = event.time
                    for (particle in mParticles) {
                        particle.move(dt.toDouble())
                    }

                    // Process event
                    if (a != null && b != null) {
                        // particle-particle collision
                        a.bounceOff(b)
                    } else a?.bounceOffVerticalWall() ?: b?.bounceOffHorizontalWall()

                    // update the priority queue with new collisions involving a or b
                    predict(a, mClock, mSimulationUpToMs)
                    predict(b, mClock, mSimulationUpToMs)
                } else {
                    mPQ.add(event)

                    // Update position.
                    for (particle in mParticles) {
                        particle.move((mClock - lastClock).toDouble())
                    }
                    break
                }
            }
        }

        // Rendering.
        draw(canvas, canvasWidth, canvasHeight, particlePaint)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    // updates priority queue with all new events for particle a
    private fun predict(thiz: Particle?,
                        currentMilliSeconds: Long,
                        upToMilliSeconds: Long) {
        if (thiz == null) return

        // particle-particle collisions
        for (that in mParticles) {
            val dt = thiz.timeToHit(that)
            if (dt == Double.POSITIVE_INFINITY || dt < 0.0) continue

            val t = currentMilliSeconds + dt
            // Overflow protection.
            if (t < 0) continue

            if (t <= upToMilliSeconds) {
                mPQ.add(Event(t.toLong(), thiz, that))
            }
        }

        // particle-wall collisions
        val dtX = thiz.timeToHitVerticalWall()
        val dtY = thiz.timeToHitHorizontalWall()
        if (dtX >= 0.0 &&
            currentMilliSeconds + dtX > 0 &&
            currentMilliSeconds + dtX <= upToMilliSeconds) {
            mPQ.add(Event((currentMilliSeconds + dtX).toLong(), thiz, null))
        }
        if (dtY >= 0.0 &&
            currentMilliSeconds + dtY > 0 &&
            currentMilliSeconds + dtY <= upToMilliSeconds) {
            mPQ.add(Event((currentMilliSeconds + dtY).toLong(), null, thiz))
        }
    }

    private fun draw(canvas: Canvas,
                     canvasWidth: Int,
                     canvasHeight: Int,
                     particlePaint: Paint) {
        for (particle in mParticles) {
            // TODO: particle shouldn't aware of the rendering details.
            // Rendering.
            particle.draw(canvas, canvasWidth, canvasHeight, particlePaint)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    /***************************************************************************
     * An event during a particle collision simulation. Each event contains
     * the time at which it will occur (assuming no supervening actions)
     * and the particles a and b involved.
     *
     * -  a and b both null:      redraw event
     * -  a null, b not null:     collision with vertical wall
     * -  a not null, b null:     collision with horizontal wall
     * -  a and b both not null:  binary collision between a and b
     */
    private class Event internal constructor(
        val time: Long,
        // time that event is scheduled to occur
        val a: Particle?,
        // particles involved in event, possibly null
        val b: Particle?) : Comparable<Event> {

        // collision counts at event creation
        private val countA: Int = a?.collisionCount ?: -1
        private val countB: Int = b?.collisionCount ?: -1

        // has any collision occurred between when event was created and now?
        internal val isValid: Boolean
            get() {
                if (a != null && a.collisionCount != countA) return false
                return if (b != null && b.collisionCount != countB) false else true
            }

        // compare times when two events will occur
        override fun compareTo(other: Event): Int {
            return when {
                this.time < other.time -> -1
                this.time > other.time -> 1
                else -> 0
            }
        }
    }

    companion object {
        private const val ONE_YEAR_MS = 365L * 24 * 60 * 60 * 100
    }
}
