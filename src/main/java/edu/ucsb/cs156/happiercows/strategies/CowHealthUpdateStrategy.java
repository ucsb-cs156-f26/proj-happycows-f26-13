package edu.ucsb.cs156.happiercows.strategies;

import edu.ucsb.cs156.happiercows.entities.GamePlus;

public interface CowHealthUpdateStrategy {

    /**
     * Calculates the new health of a group of cows (a herd) from its current
     * health and the total number of cows in the game.
     */
    public double calculateNewCowHealth(
            GamePlus gamePlus,
            double cowHealth,
            int totalCows
    );

    public String getDisplayName();
    public String getDescription();
}
