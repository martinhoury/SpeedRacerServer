/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 * Used to represent the finite state machine that is used to generate the road.
 * @author Sam
 * @version 1.0
 */
public class FiniteState {
    /**
     * The ID of the state (primary key)
     */
    public int iId;

    /**
     * The String representation of the state (used for debugging only)
     */
    public String sRepresentation;

    /**
     * The array of reachable states
     */
    public FiniteState[] nextState;

    /**
     * The array of probabilities used to randomly select the next state
     */
    public double[] dStateProb;

    /**
     * The next state with the shortest distance to the root
     */
    public FiniteState pathToRoot;

    /**
     * Constructor
     * @param iId The ID of the state (primary key)
     * @param sRepresentation The String representation of the state (used for debugging only)
     */
    public FiniteState(int iId, String sRepresentation)
    {
        this.iId = iId;
        this.sRepresentation = new String(sRepresentation);
    }

    /**
     * Randomly selects a state from the nextState array according to dStateProb probability
     * @return The randomly selected state
     */
    public FiniteState nextState()
    {
        int selected = 0;
        double dRand = Math.random();
        while(dRand > dStateProb[selected])
        {
            dRand -= dStateProb[selected];
            selected++;
        }

        return nextState[selected];
    }
}
