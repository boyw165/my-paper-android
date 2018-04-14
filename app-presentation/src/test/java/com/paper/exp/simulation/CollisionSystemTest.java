package com.paper.exp.simulation;

import com.paper.view.collisionSimulation.CollisionSystem;
import com.paper.view.collisionSimulation.Particle;

import org.junit.Assert;
import org.junit.Test;

public class CollisionSystemTest {

    @Test
    public void addition_isCorrect() throws Exception {
        final CollisionSystem tester = new CollisionSystem();

        tester.start(new Particle[]{new Particle(
            // x, y, vx, vy, radius, mass
            0.5, 0.5, 0.5, 0.5, 0.02, 0.5)});
        Assert.assertTrue(tester.getMPQ().peek().getTime() > 0f);
    }
}
