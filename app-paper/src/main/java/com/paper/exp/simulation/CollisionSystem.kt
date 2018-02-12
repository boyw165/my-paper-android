package com.paper.exp.simulation

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log

import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean

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
    private var mSimulationUpToMs: Long = 0

    // the priority queue
    private val mPQ = PriorityQueue<Event>()
    // the array of particles
    private val mParticles: MutableList<Particle> = mutableListOf()

    private val mIsReady = AtomicBoolean()

    init {
        // defensive copy
        mParticles += particles

        // Flag not ready.
        mIsReady.set(false)
    }

    fun init() {
        // TODO: Make it an interface.
        val currentMs = System.currentTimeMillis()

        mSimulationUpToMs = currentMs + ONE_YEAR_MS

        // Rebuild the event queue.
        mPQ.clear()
        for (particle in mParticles) {
            predict(particle, currentMs, mSimulationUpToMs.toDouble())
        }

        mIsReady.set(true)
    }

    /**
     * Simulates the system of particles for the specified amount of time.
     */
    fun simulate(canvas: Canvas,
                 canvasWidth: Int,
                 canvasHeight: Int,
                 particlePaint: Paint) {
        if (!mIsReady.get()) return

        Log.d("collision", "Simulation tick.")

        // TODO: The particle system shouldn't aware of the rendering details.

        // TODO: Make it an interface.
        val currentMs = System.currentTimeMillis()

        // the main event-driven simulation loop
        while (!mPQ.isEmpty()) {
            // get impending event, discard if invalidated
            val event = mPQ.peek()

            if (!event.isValid) {
                // Lazily discard the invalid events.
                mPQ.remove()
            } else if (event.time <= currentMs) {
                // Handle the particle-particle collision.
                val a = event.a
                val b = event.b

                // Physical collision, so update positions
                for (particle in mParticles) {
                    particle.move(event.time - currentMs)
                }

                // Process event
                if (a != null && b != null) {
                    // particle-particle collision
                    a.bounceOff(b)
                } else a?.bounceOffVerticalWall() ?: b?.bounceOffHorizontalWall()

                // update the priority queue with new collisions involving a or b
                predict(a, currentMs, mSimulationUpToMs.toDouble())
                predict(b, currentMs, mSimulationUpToMs.toDouble())
            } else if (event.time > currentMs) {
                break
            }
        }

        // redraw event
        for (particle in mParticles) {
            // TODO: particle shouldn't aware of the rendering details.
            particle.draw(canvas, canvasWidth, canvasHeight, particlePaint)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    // updates priority queue with all new events for particle a
    private fun predict(thiz: Particle?,
                        currentMilliSeconds: Long,
                        upToMilliSeconds: Double) {
        if (thiz == null) return

        // particle-particle collisions
        for (that in mParticles) {
            val dt = thiz.timeToHit(that)
            if (currentMilliSeconds + dt <= upToMilliSeconds) {
                mPQ.add(Event(currentMilliSeconds + dt, thiz, that))
            }
        }

        // particle-wall collisions
        val dtX = thiz.timeToHitVerticalWall()
        val dtY = thiz.timeToHitHorizontalWall()
        if (currentMilliSeconds + dtX > 0 && currentMilliSeconds + dtX <= upToMilliSeconds) {
            mPQ.add(Event(currentMilliSeconds + dtX, thiz, null))
        }
        if (currentMilliSeconds + dtX > 0 && currentMilliSeconds + dtY <= upToMilliSeconds) {
            mPQ.add(Event(currentMilliSeconds + dtY, null, thiz))
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
        val time: Double,
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
            return java.lang.Double.compare(this.time, other.time)
        }
    }

    companion object {
        private const val ONE_YEAR_MS = 365L * 24 * 60 * 60 * 100
    }
}
