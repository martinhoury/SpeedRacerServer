/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.rmi.RemoteException;
import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs most of the computations
 * @author Sam
 * @version 1.0
 */
public class Core {
    
    /**
     * The player's score
     */
    public static int score = 0;

    /**
     * The delay in ms between two loops
     */
    public int iTickDelay = 30;

    /**
     * True if the finish line has been passed. False otherwise
     */
    public static boolean bGameFinishing = false;

    /**
     * True if the player is currently playing. False otherwise
     */
    public static boolean bGameInProgress = false;

    /**
     * True if the GUI is closing. False otherwise
     */
    public static boolean bGameQuit = false;

    /**
     * True if the player is pressing the up arrow key
     */
    public static boolean UP_P = false;

    /**
     * True if the player is pressing the down arrow key
     */
    public static boolean DO_P = false;

    /**
     * True if the player is pressing the right arrow key
     */
    public static boolean RI_P = false;

    /**
     * True if the player is pressing the left arrow key
     */
    public static boolean LE_P = false;

    /**
     * The string representing the final position (rank) of the player after passing the finish line
     */
    public String sFinalPosition;

    /**
     * Integer representation of the final position (rank) of the player after passing the finish line
     */
    public int iFinalPosition;

    /**
     * The number of competitors (not civilians)
     */
    public int iNbParticipants;
   
    /**
     * Vector containing the road elements (including the obtacles)
     */
    public Vector<Rectangle>[] vTabRoad = new Vector[108];
    
    /**
     * Vector containing the obstacles that must be taken into account for collision detection (thus also includes cars)
     */
    public Vector<CollidableRectangle>[] vTabObstacles = new Vector[109];
    
    /**
     * Vector containing the cars
     */
    public Vector<Car> vCars = new Vector<Car>();

    /**
     * Vector containing the road elements to display in the sliding window (layer 1)
     */
    public Vector<Rectangle> vDisplayRoad = new Vector<Rectangle>();
    
    /**
     * Vector containing the obstacles to display in the sliding window (layer 2)
     */
    public Vector<Rectangle> vDisplayObstacles = new Vector<Rectangle>();
    
    /**
     * Vector containing the cars to display in the sliding window (layer 3)
     */
    public Vector<Rectangle> vDisplayCars = new Vector<Rectangle>();
    
    

    /**
     * The finite state arraw representing the finite state machine
     */
    private FiniteState[] fsStates;

    /**
     * The absolute position of the police car on the y-axis
     */
    private int policePos;
    
    /**
     * Used to count the time ticks (1 tick = 50ms, 20 ticks = 1 second) for score update
     */
    private int runTime = 0;

    /**
     * Same as above, but for game updates
     */
    private int gameRunTime = 0;

    /**
     * Tells the frequency of updates (gameMaxRunTime = X means one update every X ticks)
     */
    private int gameMaxRunTime = 1;
    //private Client client;
    private int client;
    private Controller controller;
   /**
    * Constructor
    * @param gGUI The reference to the graphical user interface
    */
    public Core(Controller controller,int client)
    {
        this.controller = controller;
        this.client = client;
    }


    /**
     * Initializes the finite state machine and stores it in fsStates[].
     * The probability to reach one state is given in such a way that the straight lines are preferred over the curves and appearing/disappeaing road segments.
     * The sum of probabilities must be (at least (for computational reasons); exactly equal to (for logical reasons)) 1 for each state
     */
    void initFiniteStateMachine()
    {
        //Memory allocation
        fsStates = new FiniteState[14];
        
        //The machine has 14 states.
        //For each character in the String representation of a state,
        //  - 0 means grass
        //  - 1 means road
        //  - u means the road becomes grass at the middle
        //  - ^ means the grass becoms road at the middle
        //  - \ means the road turns left (doubled due to character escaping)
        //  - / means the road turns right
        fsStates[0] = new FiniteState(0,"011110");
        fsStates[1] = new FiniteState(1,"0u1110");
        fsStates[2] = new FiniteState(2,"0111u0");
        fsStates[3] = new FiniteState(3,"0^1110");
        fsStates[4] = new FiniteState(4,"00\\\\\\0");
        fsStates[5] = new FiniteState(5,"001110");
        fsStates[6] = new FiniteState(6,"0011u0");
        fsStates[7] = new FiniteState(7,"0111^0");
        fsStates[8] = new FiniteState(8,"0///00");
        fsStates[9] = new FiniteState(9,"011100");
        fsStates[10] = new FiniteState(10,"0u1100");
        fsStates[11] = new FiniteState(11,"0^1100");
        fsStates[12] = new FiniteState(12,"0011^0");
        fsStates[13] = new FiniteState(13,"001100");
        
        fsStates[0].nextState = new FiniteState[3];
        fsStates[0].nextState[0] = fsStates[0];
        fsStates[0].nextState[1] = fsStates[1];
        fsStates[0].nextState[2] = fsStates[2];
        fsStates[0].dStateProb = new double[3];
        fsStates[0].dStateProb[0] = 0.5;
        fsStates[0].dStateProb[1] = 0.25;
        fsStates[0].dStateProb[2] = 0.25;
        fsStates[0].pathToRoot = fsStates[0];
        
        fsStates[1].nextState = new FiniteState[4];
        fsStates[1].nextState[0] = fsStates[3];
        fsStates[1].nextState[1] = fsStates[4];
        fsStates[1].nextState[2] = fsStates[5];
        fsStates[1].nextState[3] = fsStates[6];
        fsStates[1].dStateProb = new double[4];
        fsStates[1].dStateProb[0] = 0.1;
        fsStates[1].dStateProb[1] = 0.2;
        fsStates[1].dStateProb[2] = 0.5;
        fsStates[1].dStateProb[3] = 0.2;
        fsStates[1].pathToRoot = fsStates[3];
        
        fsStates[2].nextState = new FiniteState[4];
        fsStates[2].nextState[0] = fsStates[7];
        fsStates[2].nextState[1] = fsStates[8];
        fsStates[2].nextState[2] = fsStates[9];
        fsStates[2].nextState[3] = fsStates[10];
        fsStates[2].dStateProb = new double[4];
        fsStates[2].dStateProb[0] = 0.1;
        fsStates[2].dStateProb[1] = 0.2;
        fsStates[2].dStateProb[2] = 0.5;
        fsStates[2].dStateProb[3] = 0.2;
        fsStates[2].pathToRoot = fsStates[7];
        
        fsStates[3].nextState = new FiniteState[3];
        fsStates[3].nextState[0] = fsStates[0];
        fsStates[3].nextState[1] = fsStates[1];
        fsStates[3].nextState[2] = fsStates[2];
        fsStates[3].dStateProb = new double[3];
        fsStates[3].dStateProb[0] = 0.5;
        fsStates[3].dStateProb[1] = 0.2;
        fsStates[3].dStateProb[2] = 0.3;
        fsStates[3].pathToRoot = fsStates[0];

        fsStates[4].nextState = new FiniteState[4];
        fsStates[4].nextState[0] = fsStates[7];
        fsStates[4].nextState[1] = fsStates[8];
        fsStates[4].nextState[2] = fsStates[9];
        fsStates[4].nextState[3] = fsStates[10];
        fsStates[4].dStateProb = new double[4];
        fsStates[4].dStateProb[0] = 0.1;
        fsStates[4].dStateProb[1] = 0.2;
        fsStates[4].dStateProb[2] = 0.5;
        fsStates[4].dStateProb[3] = 0.2;
        fsStates[4].pathToRoot = fsStates[7];

        fsStates[5].nextState = new FiniteState[4];
        fsStates[5].nextState[0] = fsStates[3];
        fsStates[5].nextState[1] = fsStates[4];
        fsStates[5].nextState[2] = fsStates[5];
        fsStates[5].nextState[3] = fsStates[6];
        fsStates[5].dStateProb = new double[4];
        fsStates[5].dStateProb[0] = 0.17;
        fsStates[5].dStateProb[1] = 0.17;
        fsStates[5].dStateProb[2] = 0.5;
        fsStates[5].dStateProb[3] = 0.16;
        fsStates[5].pathToRoot = fsStates[3];

        fsStates[6].nextState = new FiniteState[3];
        fsStates[6].nextState[0] = fsStates[11];
        fsStates[6].nextState[1] = fsStates[12];
        fsStates[6].nextState[2] = fsStates[13];
        fsStates[6].dStateProb = new double[3];
        fsStates[6].dStateProb[0] = 0.3;
        fsStates[6].dStateProb[1] = 0.2;
        fsStates[6].dStateProb[2] = 0.5;
        fsStates[6].pathToRoot = fsStates[11];

        fsStates[7].nextState = new FiniteState[3];
        fsStates[7].nextState[0] = fsStates[0];
        fsStates[7].nextState[1] = fsStates[1];
        fsStates[7].nextState[2] = fsStates[2];
        fsStates[7].dStateProb = new double[3];
        fsStates[7].dStateProb[0] = 0.5;
        fsStates[7].dStateProb[1] = 0.3;
        fsStates[7].dStateProb[2] = 0.2;
        fsStates[7].pathToRoot = fsStates[0];

        fsStates[8].nextState = new FiniteState[4];
        fsStates[8].nextState[0] = fsStates[3];
        fsStates[8].nextState[1] = fsStates[4];
        fsStates[8].nextState[2] = fsStates[5];
        fsStates[8].nextState[3] = fsStates[6];
        fsStates[8].dStateProb = new double[4];
        fsStates[8].dStateProb[0] = 0.2;
        fsStates[8].dStateProb[1] = 0.1;
        fsStates[8].dStateProb[2] = 0.5;
        fsStates[8].dStateProb[3] = 0.2;
        fsStates[8].pathToRoot = fsStates[3];

        fsStates[9].nextState = new FiniteState[4];
        fsStates[9].nextState[0] = fsStates[7];
        fsStates[9].nextState[1] = fsStates[8];
        fsStates[9].nextState[2] = fsStates[9];
        fsStates[9].nextState[3] = fsStates[10];
        fsStates[9].dStateProb = new double[4];
        fsStates[9].dStateProb[0] = 0.17;
        fsStates[9].dStateProb[1] = 0.17;
        fsStates[9].dStateProb[2] = 0.5;
        fsStates[9].dStateProb[3] = 0.16;
        fsStates[9].pathToRoot = fsStates[7];
        
        fsStates[10].nextState = new FiniteState[3];
        fsStates[10].nextState[0] = fsStates[11];
        fsStates[10].nextState[1] = fsStates[12];
        fsStates[10].nextState[2] = fsStates[13];
        fsStates[10].dStateProb = new double[3];
        fsStates[10].dStateProb[0] = 0.2;
        fsStates[10].dStateProb[1] = 0.3;
        fsStates[10].dStateProb[2] = 0.5;
        fsStates[10].pathToRoot = fsStates[11];
        
        fsStates[11].nextState = new FiniteState[4];
        fsStates[11].nextState[0] = fsStates[7];
        fsStates[11].nextState[1] = fsStates[8];
        fsStates[11].nextState[2] = fsStates[9];
        fsStates[11].nextState[3] = fsStates[10];
        fsStates[11].dStateProb = new double[4];
        fsStates[11].dStateProb[0] = 0.2;
        fsStates[11].dStateProb[1] = 0.2;
        fsStates[11].dStateProb[2] = 0.5;
        fsStates[11].dStateProb[3] = 0.1;
        fsStates[11].pathToRoot = fsStates[7];
        
        fsStates[12].nextState = new FiniteState[4];
        fsStates[12].nextState[0] = fsStates[3];
        fsStates[12].nextState[1] = fsStates[4];
        fsStates[12].nextState[2] = fsStates[5];
        fsStates[12].nextState[3] = fsStates[6];
        fsStates[12].dStateProb = new double[4];
        fsStates[12].dStateProb[0] = 0.2;
        fsStates[12].dStateProb[1] = 0.2;
        fsStates[12].dStateProb[2] = 0.5;
        fsStates[12].dStateProb[3] = 0.1;
        fsStates[12].pathToRoot = fsStates[3];
        
        fsStates[13].nextState = new FiniteState[3];
        fsStates[13].nextState[0] = fsStates[11];
        fsStates[13].nextState[1] = fsStates[12];
        fsStates[13].nextState[2] = fsStates[13];
        fsStates[13].dStateProb = new double[3];
        fsStates[13].dStateProb[0] = 0.25;
        fsStates[13].dStateProb[1] = 0.25;
        fsStates[13].dStateProb[2] = 0.5;
        fsStates[13].pathToRoot = fsStates[11];


    }

    /**
     * Performs some calibration measure such that the game looks smooth
     * @return The delay in ms that we must wait between two loops
     */
    int computeTickValueForCurrentSystem()
    {
        //Minimum and maximum value for tick
        int min = 1;
        int max = 100;

        //Init mean value
        int rc = 50;

        //Loop until convergence
        while(max-min > 1)
        {
            rc = (max+min)/2;

            System.out.println("Starting calibration");
            long lNanoTime = System.nanoTime();

            //Perform 20 loops
            //Initializes the game status booleans
            bGameQuit = false;
            bGameInProgress = false;

            for(int i = 0; i < 20; i++)
            {
                try
                {
                    //Wait for a tick
                    Thread.sleep(rc);

                    //Count this new tick
                    runTime++;
                    gameRunTime++;

                    //If we must update the game status
                    if(gameRunTime%gameMaxRunTime == 0 && bGameInProgress == true)
                    {

                        //Move the cars according to their speed, acceleration and to the pressed keys (for player car only)
                        moveCars(UP_P, DO_P, LE_P, RI_P, vCars);

                        //Manage the collisions (the finish line is a CollidableRectangle, so it also tells whether the game must end soon)
                        bGameFinishing = manageCollisions(vCars, vTabObstacles, bGameFinishing);

                        //Re-initialize the vectors for display rectangles
                        vDisplayRoad = new Vector<Rectangle>();
                        vDisplayObstacles = new Vector<Rectangle>();
                        vDisplayCars = new Vector<Rectangle>();

                        //Find the rectangles to display according to the car speed and position
                        findDisplayRectangles(vTabRoad, vTabObstacles, vCars, vDisplayRoad, vDisplayObstacles, vDisplayCars);

                        //Get position (rank)
                        if(!bGameFinishing)
                        {
                            int pos = 1;
                            int ypos = (int)vCars.elementAt(0).y;
                            Iterator<Car> iCars = vCars.iterator();
                            Car temp = iCars.next(); //Skip first car (player car)
                            if(temp.bustedTime > 0)
                                temp.bustedTime--;
                            while(iCars.hasNext())
                            {
                                Car currentCar = iCars.next();
                                if(currentCar.Racer)
                                {
                                    if(currentCar.y < ypos)
                                        pos++;
                                }
                                if(currentCar.bustedTime > 0)
                                    currentCar.bustedTime--;
                            }
                            iFinalPosition = pos;
                        }

                    }

                    //The score updates every second if the game is running by adding the square of the current player car speed
                    if(runTime == 20)
                    {
                        runTime = 0;
                        if(bGameInProgress == true)
                            score+=Math.pow(vCars.elementAt(0).ySpeed, 2);
                    }
                }
                catch(Exception e)
                {
                    //In case of problem, we only display the exception, but we keep going
                    e.printStackTrace();
                }
            }

            long lNanoTime2 = System.nanoTime();
            long diff = lNanoTime2 - lNanoTime;

            System.out.println("Calibration : " + diff + ", rc = " + rc);
            
            if(diff > 500000000)
                max = rc;
            else
                min = rc;
        }
        
        return rc;
    }

    /**
     * "Main" method of the class. It loops until the GUI asked for closure. After each loop, the thread sleeps for 50 ms. In each loop:
     * - The cars are moved
     * - The collisions are processed
     * - The rectangles to display are computed
     * - The player position (rank) is computed
     * - The GUI is asked to perform its update
     */
    public void runGame()
    {
        //Initialize the finite state machine
        initFiniteStateMachine();

        //Generates the road, obstacles and cars
        newGrid();

        iTickDelay = computeTickValueForCurrentSystem();
        try {
            controller.getClientHashMap().get(client).getCallback().enablePlayButton(true);
            //this.getClient().getCallback().enablePlayButton(true);
            //gGUI.jButton1.setEnabled(true);
        } catch (RemoteException ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        //Initializes the game status booleans
        bGameQuit = false;
        bGameInProgress = false;

        //While the GUI did not ask for closure
        while(bGameQuit == false)
        {
            try
            {
                //Wait for a tick
                Thread.sleep(iTickDelay);

                //Count this new tick
                runTime++;
                gameRunTime++;

                //If we must update the game status
                if(gameRunTime%gameMaxRunTime == 0 && bGameInProgress == true)
                {
                    
                    //Move the cars according to their speed, acceleration and to the pressed keys (for player car only)
                    moveCars(UP_P, DO_P, LE_P, RI_P, vCars);
                    
                    //Manage the collisions (the finish line is a CollidableRectangle, so it also tells whether the game must end soon)
                    bGameFinishing = manageCollisions(vCars, vTabObstacles, bGameFinishing);

                    //Re-initialize the vectors for display rectangles
                    vDisplayRoad = new Vector<Rectangle>();
                    vDisplayObstacles = new Vector<Rectangle>();
                    vDisplayCars = new Vector<Rectangle>();

                    //Find the rectangles to display according to the car speed and position
                    findDisplayRectangles(vTabRoad, vTabObstacles, vCars, vDisplayRoad, vDisplayObstacles, vDisplayCars);

                    //Get position (rank)
                    if(!bGameFinishing)
                    {
                        int pos = 1;
                        int ypos = (int)vCars.elementAt(0).y;
                        Iterator<Car> iCars = vCars.iterator();
                        Car temp = iCars.next(); //Skip first car (player car)
                        if(temp.bustedTime > 0)
                            temp.bustedTime--;
                        while(iCars.hasNext())
                        {
                            Car currentCar = iCars.next();
                            if(currentCar.Racer)
                            {
                                if(currentCar.y < ypos)
                                    pos++;
                            }
                            if(currentCar.bustedTime > 0)
                                currentCar.bustedTime--;
                        }
                        iFinalPosition = pos;
                    }

                    //Ask the GUI to perform its update
                    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                        @Override public void run() {
                            try {
                                controller.getClientHashMap().get(client).getCallback().update(vDisplayRoad, vDisplayObstacles, vDisplayCars, vCars.elementAt(0), iFinalPosition, iNbParticipants, bGameFinishing, sFinalPosition);
                                //getClient().getCallback().update(vDisplayRoad, vDisplayObstacles, vDisplayCars, vCars.elementAt(0), iFinalPosition, iNbParticipants, bGameFinishing, sFinalPosition);
                                //gGUI.update(vDisplayRoad, vDisplayObstacles, vDisplayCars, vCars.elementAt(0), iFinalPosition, iNbParticipants, bGameFinishing, sFinalPosition);
                            } catch (RemoteException ex) {
                                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });

                }

                //The score updates every second if the game is running by adding the square of the current player car speed
                if(runTime == 20)
                {
                    runTime = 0;
                    if(bGameInProgress == true)
                        score+=Math.pow(vCars.elementAt(0).ySpeed, 2);
                }
            }
            catch(Exception e)
            {
                //In case of problem, we only display the exception, but we keep going
                e.printStackTrace();
            }
        }
    }

    /**
     * Manages the collisions
     * @param Cars The vector of cars (which are the actors of the collision)
     * @param vObstacles The vector of obstacles (which are passively collided). Also contains the cars
     * @param bGameFinishing True if the finish line has been passed
     * @return True if the finish line has just been passed. False otherwise
     */
    public boolean manageCollisions(Vector<Car> Cars, Vector<CollidableRectangle>[] vTabObstacles, boolean bGameFinishing)
    {
        //Is the finish line been passed?
        boolean bgf = bGameFinishing;

        //For each Car
        Iterator<Car> iCar = vCars.iterator();
        while(iCar.hasNext())
        {
            //Current car of the loop
            Car myCar = iCar.next();

            //In which section am I?
            int iSector = (int)myCar.y/400;
            if(iSector < 1)
                iSector = 1;
            else if(iSector >= 107)
                iSector = 106;

            //Generate the vector of obstacles which can be hit by the current car
            Vector<CollidableRectangle> vCloseObstacles = new Vector<CollidableRectangle>(vTabObstacles[iSector-1]);
            vCloseObstacles.addAll(vTabObstacles[iSector]);
            vCloseObstacles.addAll(vTabObstacles[iSector+1]);

            //Also add the cars
            vCloseObstacles.addAll(vTabObstacles[108]);

            //For each obstacle
            Iterator<CollidableRectangle> iObstacles = vCloseObstacles.iterator();
            while(iObstacles.hasNext())
            {
                //Current obstacle
                CollidableRectangle currentObstacle = iObstacles.next();

                //Find the intersection between the current car and the current obstacle
                Rectangle rInter = findIntersection(myCar, currentObstacle);

                //If there is an intersection
                if(rInter != null)
                {
                    //Switch from relative position to absolute position (on the y-axis)
                    rInter.y += myCar.y;

                    if(currentObstacle.effect == 3 && myCar.id == 6)
                    {
                        //The player just passed the finish line
                        if(!bgf)
                        {
                            bgf = true;

                            //Update the String representation of the rank
                            if(iFinalPosition == 1)
                            {
                                sFinalPosition = "1st";
                            }
                            else if(iFinalPosition == 2)
                            {
                                sFinalPosition = "2nd";
                            }
                            else if(iFinalPosition == 3)
                            {
                                sFinalPosition = "3rd";
                            }
                            else
                            {
                                sFinalPosition = new String(iFinalPosition+"th");
                            }
                        }

                    }
                    else if(currentObstacle.effect == 4)
                    {
                        //One car hit the flash zone. The flash is set to be active at 3.6 (thus 180 Km/h)
                        // in such a way that only the player car can be busted (or another car if pushed by the player)
                        if(myCar.ySpeed >= 3.6)
                        {
                            //This car is busted for 5 seconds
                            myCar.bustedTime = 100;
                            myCar.bustedSpeed = (int)(myCar.ySpeed*50);

                            //We put this car behing the police car and prevent it for moving during the 5 second penalty
                            myCar.y = policePos+64;
                            myCar.x = 292;
                            myCar.xSpeed = 0;
                            myCar.xAcc = 0;
                            myCar.ySpeed = 0;
                            if(myCar.id == 6)
                            {
                                LE_P = false;
                                RI_P = false;
                            }
                        }
                    }
                    else if(currentObstacle.effect == 2)
                    {
                        //The car is slowed down (by the grass)
                        myCar.ySpeed = myCar.ySpeed*0.97;
                    }
                    else if(currentObstacle.effect == 1)
                    {
                        //The car hit a movable obstacle (another car)
                        //Check that the two cars are different
                        if(!myCar.equals(currentObstacle))
                        {
                            //Find the places where the car hit the obstacle
                            boolean[] bHitPlace = findHitPlace(myCar,rInter);

                            //We hit on the front part. We transfer the speed on the y axis to the obstacle and reversely then we move the car a bit backwards
                            if(bHitPlace[1] == true)
                            {
                                if(myCar.ySpeed > ((Car)currentObstacle).ySpeed)
                                {
                                    double temp = myCar.ySpeed;
                                    myCar.ySpeed = ((Car)currentObstacle).ySpeed;
                                    ((Car)currentObstacle).ySpeed = temp;
                                }
                                myCar.y += rInter.height;
                            }

                            //We hit on the plain left or plain right. We transfer the speed on the x axis to the obstacle and reversely then we move the car a bit to the opposite direction
                            else if(bHitPlace[3] == true || bHitPlace[5] == true)
                            {
                                double temp = myCar.xSpeed;
                                myCar.xSpeed = ((Car)currentObstacle).xSpeed;
                                ((Car)currentObstacle).xSpeed = temp;
                                if(bHitPlace[3] == true)
                                    myCar.x += rInter.width;
                                else
                                    myCar.x -= rInter.width;
                            }

                            //We only hit on a top corner. We do both the above
                            else if(bHitPlace[0] == true || bHitPlace[2] == true)
                            {
                                if(myCar.ySpeed > ((Car)currentObstacle).ySpeed)
                                {
                                    double temp = myCar.ySpeed;
                                    myCar.ySpeed = ((Car)currentObstacle).ySpeed;
                                    ((Car)currentObstacle).ySpeed = temp;
                                }
                                double temp = myCar.xSpeed;
                                myCar.xSpeed = ((Car)currentObstacle).xSpeed;
                                ((Car)currentObstacle).xSpeed = temp;
                                myCar.y += rInter.height;
                                if(bHitPlace[0] == true)
                                    myCar.x += rInter.width;
                                else
                                    myCar.x -= rInter.width;
                            }
                        }
                    }
                    else if(currentObstacle.effect == 0)
                    {
                        //The car hit an obstacle that cannot move
                        //Find the places where the car hit the obstacle
                        boolean[] bHitPlace = findHitPlace(myCar,rInter);

                        //We hit on the front part. The car will stop immediately.
                        if(bHitPlace[1] == true)
                        {
                            myCar.ySpeed = 0;
                            myCar.y += rInter.height;
                        }

                        //We hit on the sides
                        else if(bHitPlace[0] == true || bHitPlace[2] == true || bHitPlace[3] == true || bHitPlace[5] == true)
                        {
                            //If we hit on the top corners, we slow down a bit more than if we hit on the plain sides
                            if(bHitPlace[0] == true || bHitPlace[2] == true)
                                myCar.ySpeed = myCar.ySpeed*0.95;
                            else
                                myCar.ySpeed = myCar.ySpeed*0.97;

                            //We move the car to the opposite direction
                            if(bHitPlace[0] == true || bHitPlace[3] == true)
                                myCar.x += rInter.width;
                            else
                                myCar.x -= rInter.width;

                        }
                    }
                }
            }
        }
        
        return bgf;
    }

    /**
     * Find the area of the car that is hit by the obstacle
     * @param myCar The car that made the collision
     * @param rInter The intersection of the car and the obstacle
     * @return The array of collided zones:
     * +---+---+---+
     * | 0 | 1 | 2 |
     * +---+---+---+
     * | 3 | 4 | 5 |
     * +---+---+---+
     * | 6 | 7 | 8 |
     * +---+---+---+
     * Note : Only the regions 0, 1, 2, 3 and 5 are computed
     */
    public boolean[] findHitPlace(Rectangle myCar, Rectangle rInter)
    {
        boolean[] rb = new boolean[6];
        
        rb[0] = findIntersection(new Rectangle(myCar.x,myCar.y,10,21,6),rInter) != null;
        rb[1] = findIntersection(new Rectangle(myCar.x+10,myCar.y,12,21,6),rInter) != null;
        rb[2] = findIntersection(new Rectangle(myCar.x+22,myCar.y,10,21,6),rInter) != null;
        rb[3] = findIntersection(new Rectangle(myCar.x,myCar.y+21,10,22,6),rInter) != null;
        //rb[4] = findIntersection(new Rectangle(myCar.x+10,myCar.y+21,12,22,6),rInter) != null;
        rb[5] = findIntersection(new Rectangle(myCar.x+22,myCar.y+21,10,22,6),rInter) != null;
        //rb[6] = findIntersection(new Rectangle(myCar.x,myCar.y+43,10,21,6),rInter) != null;
        //rb[7] = findIntersection(new Rectangle(myCar.x+10,myCar.y+43,12,21,6),rInter) != null;
        //rb[8] = findIntersection(new Rectangle(myCar.x+22,myCar.y+43,10,21,6),rInter) != null;
        
        return rb;
    }

    /**
     * Moves the cars according to their speed, acceleration and key pressed for the player's car
     * @param UP_P True if the up arrow is being pressed
     * @param DO_P True if the down arrow is being pressed
     * @param LE_P True if the left arrow is being pressed
     * @param RI_P True if the right arrow is being pressed
     * @param vCars The vector of cars to move
     */
    public void moveCars(boolean UP_P, boolean DO_P, boolean LE_P, boolean RI_P, Vector<Car> vCars)
    {
        //Extract the player's car (always at position 0 in the vector!)
        Car myCar = vCars.elementAt(0);

        //If we did not pass the finish line, we can still act on the acceleration on the y axis
        if(!bGameFinishing)
        {
            if(UP_P)
            {
                //Accelerates
                if(myCar.yAcc < 4)
                    myCar.yAcc++;
            }
            else if(DO_P)
            {
                //Decelerates
                if(myCar.yAcc > -8)
                    myCar.yAcc--;
            }
            else
            {
                //Iteratively reachs a constant deceleration of -1 if no key is pressed
                if(myCar.yAcc > -1)
                    myCar.yAcc--;
                else if(myCar.yAcc < -1)
                    myCar.yAcc++;
            }
        }
        else
        {
            //If we passed the finish line, we must decelerate
            myCar.yAcc = -8;
        }

        //Impacts the acceleration of the x axis
        if(RI_P)
        {
            //Going to the right
            if(myCar.xAcc < 4)
                myCar.xAcc++;
        }
        else if(LE_P)
        {
            //Going to the left
            if(myCar.xAcc > -4)
                myCar.xAcc--;
        }
        else
        {
            //If we don't press anything, the x acceleration is calculated to iteratively counter the x speed and make it reach 0
            if(myCar.xSpeed > 1)
            {
                myCar.xAcc = -(int)(myCar.xSpeed+1);
                if(myCar.xAcc < -4)
                    myCar.xAcc = -4;
            }
            else if(myCar.xSpeed < -1)
            {
                myCar.xAcc = -(int)(myCar.xSpeed-1);
                if(myCar.xAcc > 4)
                    myCar.xAcc = 4;
            }
            else
            {
                myCar.xAcc = 0;
                myCar.xSpeed = 0;
            }
        }
        
        //We then scan the other cars
        Iterator<Car> iCars = vCars.iterator();
        while(iCars.hasNext())
        {
            Car currentCar = iCars.next();

            //If this is not the player's car
            if(currentCar.id != 6)
            {
                //We try to maintain the x speed to 0 using the same formula
                if(currentCar.xSpeed > 1)
                {
                    currentCar.xAcc = -(int)(currentCar.xSpeed+1);
                }
                else if(currentCar.xSpeed < -1)
                {
                    currentCar.xAcc = -(int)(currentCar.xSpeed-1);
                }
                else
                {
                    currentCar.xAcc = 0;
                    currentCar.xSpeed = 0;
                }
            }

            //The speed on the y axis is updated according to the acceleration
            currentCar.ySpeed += (double)currentCar.yAcc/100;

            //We try to maintain the car speed in its acceptable range of functionning
            if(currentCar.ySpeed < 0.5)
            {
                //The car must have at least a speed of 0.5
                if(!bGameFinishing)
                {
                    currentCar.ySpeed = 0.5;
                }
                else
                {
                    //Unless the player passes the finish line which, at this point, stops the game
                    if(currentCar.id == 6)
                    {
                        currentCar.ySpeed = 0;
                        bGameInProgress = false;
                    }
                    else
                    {
                        currentCar.ySpeed = 0.5;
                    }
                }
            }
            //The opponents will try to stay at 3.58
            else if(currentCar.ySpeed > 3.5 && currentCar.Racer && currentCar.id == 7)
            {
                currentCar.ySpeed = (currentCar.ySpeed - 3.5)*0.8 + 3.5;
            }
            //The player car cannot exceed a speed of 4.64
            else if(currentCar.ySpeed > 4.5 && currentCar.Racer && currentCar.id == 6)
                currentCar.ySpeed = (currentCar.ySpeed - 4.5)*0.8 + 4.5;
            //The civilans will try to stay at 2.04
            else if(currentCar.ySpeed > 2 && !currentCar.Racer)
                currentCar.ySpeed = (currentCar.ySpeed - 2)*0.8 + 2;


            //The speed on the x axis is updated with the acceleration
            currentCar.xSpeed += (double)currentCar.xAcc/5;

            //The speed will stay in a certain range [-8,8]
            if(currentCar.xSpeed > 8)
                currentCar.xSpeed = 8;
            if(currentCar.xSpeed < -8)
                currentCar.xSpeed = -8;

            //We update the car position according to its speeds
            currentCar.y -= currentCar.ySpeed*4;
            currentCar.x += currentCar.xSpeed;

            //But the position of the car must be in the range [0,368]
            if(currentCar.x < 0)
            {
                currentCar.x = 0;
                currentCar.xAcc = 0;
                currentCar.xSpeed = 0;
            }
            if(currentCar.x > 368)
            {
                currentCar.x = 368;
                currentCar.xAcc = 0;
                currentCar.xSpeed = 0;
            }

            //If, for some reason, the player car did not decrease enough after the finish line, we ensure that the game will end
            //before we get out of the visible road (+ a safety margin)
            if(bGameFinishing && currentCar.id == 6 && currentCar.y < 500)
            {
                bGameInProgress = false;
            }
            
        }
    }

    /**
     * Extracts the visible rectangles to display according to the player's car position and speed
     * @param vRoad The vector of road elements
     * @param vObstacles The vector of obstacles
     * @param vCars The vector of cars
     * @param vDisplayRoad The vector of road elements to display (updated)
     * @param vDisplayObstacles The vector of collision warning rectagles (updated)
     * @param vDisplayCars The vector of cars to display (updated)
     */
    public void findDisplayRectangles(Vector<Rectangle>[] vTabRoad, Vector<CollidableRectangle>[] vTabObstacles, Vector<Car> vCars,
            Vector<Rectangle> vDisplayRoad, Vector<Rectangle> vDisplayObstacles, Vector<Rectangle> vDisplayCars)
    {

        //Where is the display window?
        int myCarY = 0;
        Car currentCar = vCars.elementAt(0);
        myCarY = (int)currentCar.y;           //Position on the y axis of the car on the road

        //Where is the car w.r.t the display according to its speed?
        int displayCarY = (int)((4.5-currentCar.ySpeed)*50)+100;

        //Where is the display window with respect to the road?
        int displayBoxY = myCarY-displayCarY;

        //We construct the rectangle that represents the current view
        Rectangle currentView = new Rectangle(0,displayBoxY,400,400,9);

        //In which section am I?
        int iSector = displayBoxY/400;
        if(iSector == 0)
            iSector = 1;
        else if(iSector == 107)
            iSector = 106;

        //Generate the vector of road elements which can be seen by the current car
        Vector<Rectangle> vCloseRoad = new Vector<Rectangle>(vTabRoad[iSector-1]);
        vCloseRoad.addAll(vTabRoad[iSector]);
        vCloseRoad.addAll(vTabRoad[iSector+1]);

        //For each road item
        Iterator<Rectangle> iRoadElem = vCloseRoad.iterator();
        while(iRoadElem.hasNext())
        {
            //If this item intersects with the display window
            Rectangle rInter = findIntersection(currentView,iRoadElem.next());
            if(rInter != null)
            {
                //We display it
                vDisplayRoad.add(rInter);
            }
        }

        //For each car
        Iterator<Car> iCarElem = vCars.iterator();
        while(iCarElem.hasNext())
        {
            //If this car intersects with the display window
            Rectangle rInter = findIntersection(currentView,iCarElem.next());
            if(rInter != null)
            {
                //We display it
                vDisplayCars.add(rInter);
            }
        }

        //We construct four rectangles that are on top of the display window and have a size of 100 pixels each
        Rectangle[] upperView = new Rectangle[4];
        upperView[0] = new Rectangle(0,displayBoxY-400,400,100,9);
        upperView[1] = new Rectangle(0,displayBoxY-300,400,100,9);
        upperView[2] = new Rectangle(0,displayBoxY-200,400,100,9);
        upperView[3] = new Rectangle(0,displayBoxY-100,400,100,9);

        //Which means that the sector is upper
        iSector--;
        if(iSector == 0)
            iSector = 1;
        
        //Generate the vector of obstacles which can be hit by the current car
        Vector<CollidableRectangle> vCloseObstacles = new Vector<CollidableRectangle>(vTabObstacles[iSector-1]);
        vCloseObstacles.addAll(vTabObstacles[iSector]);
        vCloseObstacles.addAll(vTabObstacles[iSector+1]);
        
        //Including the cars
        vCloseObstacles.addAll(vTabObstacles[108]);

        //For each obstacle
        Iterator<CollidableRectangle> iObstacleElem = vCloseObstacles.iterator();
        while(iObstacleElem.hasNext())
        {
            CollidableRectangle crCurrent = iObstacleElem.next();
            for(int i = 0; i < 4; i++)
            {
                //If there is an intersection of one of the rectangles above the display window
                Rectangle rInter = findIntersection(upperView[i],crCurrent);
                if(rInter != null && rInter.id >= 5 && rInter.id != 13)
                {
                    //We display a red rectangle representing the danger to collide with oncoming obstable (except grass, trees and flash zones)
                    //The closer the object is, the wider the red rectangle will be
                    vDisplayObstacles.add(new Rectangle(rInter.x,0.0,rInter.width,i+1,10));
                }
            }
        }

    }

    /**
     * Finds the intersection, if any, between two rectangles
     * @param reference The reference rectangle (which originates the collision)
     * @param candidate The candidate for collision (passive)
     * @return The intersection between the two rectangles in relative coordinates, or null of the two rectangles don't intersect
     */
    public Rectangle findIntersection(Rectangle reference, Rectangle candidate)
    {
        //Originally, the intersection is null
        Rectangle rr = null;

        //We use java.awt.Rectangle to construct two rectangles of the same dimensions as the parameters
        java.awt.Rectangle r1 = new java.awt.Rectangle((int)reference.x,(int)reference.y,reference.width, reference.height);
        java.awt.Rectangle r2 = new java.awt.Rectangle((int)candidate.x,(int)candidate.y,candidate.width, candidate.height);
        java.awt.Rectangle inter = null;

        //If they intersect, use the built-in method to find their intersection
        if(r1.intersects(r2))
            inter = r1.intersection(r2);

        //If they intersect
        if(inter != null && inter.y-(int)reference.y >= 0)
        {
            //Constructs a new Rectangle with this intersection (the y coordinate is relative the the reference)
            rr = new Rectangle(inter.x, inter.y-(int)reference.y,inter.width, inter.height,candidate.id);
        }
        
        return rr;
    }

    /**
     * Uses the randomly selected state to generate a new road segment of size 400x400
     * @param vRoad The vector of road elements (updated)
     * @param vObstacles The vector of obstacles (updated)
     * @param iSegmentId The id of the state that were randomly selected by the finite state machine
     * @param offset The offset to apply to the 400x400 segment on the y axis
     */
    public void generateNextRoadSegment(Vector<Rectangle>[] vTabRoad, Vector<CollidableRectangle>[] vTabObstacles, int iSegmentId, int offset)
    {
        //The machine has 14 states.
        //For each character in the String representation of a state,
        //  - 0 means grass
        //  - 1 means road
        //  - u means the road becomes grass at the middle
        //  - ^ means the grass becoms road at the middle
        //  - \ means the road turns left (doubled due to character escaping)
        //  - / means the road turns right

        //What segment location do I need to generate
        int iSegmentLocation = offset/400;

        //Create the new vectors for this segment
        vTabRoad[iSegmentLocation] = new Vector<Rectangle>();
        Vector<Rectangle> vRoad = vTabRoad[iSegmentLocation];
        vTabObstacles[iSegmentLocation] = new Vector<CollidableRectangle>();
        Vector<CollidableRectangle> vObstacles = vTabObstacles[iSegmentLocation];

        //Generate the road segment according to the selected ID
        if(iSegmentId == 0)
        {
            //011110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset,172,400,1)); //Route noire
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            if(Math.random() >= 0.8)
            {
                crTemp = new CollidableRectangle(114,offset+200,24,32,5,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
            }
        }
        else if(iSegmentId == 1)
        {
            //0u1110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset,44,200,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset+200,4,200,3)); //Bord à gauche
            vRoad.add(new Rectangle(154,offset,4,200,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset+200,172,200,1)); //Route noire
            vRoad.add(new Rectangle(158,offset,128,200,1)); //Route noire bis
            vRoad.add(new Rectangle(154,offset+200,4,200,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 5; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 5; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            crTemp = new CollidableRectangle(110,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(134,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 2)
        {
            //0111u0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,44,200,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(242,offset,4,200,3)); //Bord à droite bis
            vRoad.add(new Rectangle(286,offset+200,4,200,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset+200,172,200,1)); //Route noire
            vRoad.add(new Rectangle(114,offset,128,200,1)); //Route noire bis
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset+200,4,200,2)); //Séparateur témoin 3

            for(int i = 0; i < 5; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 5; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            crTemp = new CollidableRectangle(242,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(266,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 3)
        {
            //0^1110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset+100,11,300,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset+150,11,250,0,2));  //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset+200,11,200,0,2));  //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset+250,11,150,0,2));  //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(121,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(143,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(154,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,128,400,1)); //Route noire
            vRoad.add(new Rectangle(147,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(125,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(114,offset,11,100,1)); //Route noire pent
            vRoad.add(new Rectangle(154,offset,4,240,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 4)
        {
            //00\\\0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset+100,11,300,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset+150,11,250,0,2));  //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset+200,11,200,0,2));  //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset+250,11,150,0,2));  //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset,11,250,0,2)); //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset,11,200,0,2)); //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset,11,150,0,2)); //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,11,100,0,2)); //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(121,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(143,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(154,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(242,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(253,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(275,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(286,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(114,offset,128,100,1)); //Route noire
            vRoad.add(new Rectangle(125,offset+100,128,50,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset+150,128,50,1)); //Route noire ter
            vRoad.add(new Rectangle(147,offset+200,128,50,1)); //Route noire quad
            vRoad.add(new Rectangle(158,offset+250,128,150,1)); //Route noire pent

            for(int i = 0; i < 3; i++)
            {
                vRoad.add(new Rectangle(154,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 3; i < 7; i++)
            {
                vRoad.add(new Rectangle(154+11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(198+11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 7; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(242,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 5)
        {
            //001110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,128,400,1)); //Route noire
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            if(Math.random() >= 0.8)
            {
                crTemp = new CollidableRectangle(262,offset+200,24,32,5,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
            }
        }
        else if(iSegmentId == 6)
        {
            //0011u0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset+200,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,200,0,2)); //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(286,offset+200,4,200,3)); //Bord à droite
            vRoad.add(new Rectangle(242,offset,4,200,3)); //Bord à droite bis
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            vRoad.add(new Rectangle(246,offset+200,40,400,1)); //Route noire bis
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset+200,4,200,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }

            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            crTemp = new CollidableRectangle(242,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(266,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 7)
        {
            //0111^0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset+100,11,300,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset+150,11,250,0,2));  //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset+200,11,200,0,2));  //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset+250,11,150,0,2));  //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(275,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(253,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(242,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(286,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset,128,400,1)); //Route noire
            vRoad.add(new Rectangle(242,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(253,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(264,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(275,offset,11,100,1)); //Route noire pent
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,240,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 8)
        {
            //0///00
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset+100,11,300,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset+150,11,250,0,2));  //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset+200,11,200,0,2));  //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset+250,11,150,0,2));  //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset,11,250,0,2)); //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset,11,200,0,2)); //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset,11,150,0,2)); //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset,11,100,0,2)); //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(143,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(121,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(110,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(286,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(275,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(253,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(242,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(158,offset,128,100,1)); //Route noire
            vRoad.add(new Rectangle(147,offset+100,128,50,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset+150,128,50,1)); //Route noire ter
            vRoad.add(new Rectangle(125,offset+200,128,50,1)); //Route noire quad
            vRoad.add(new Rectangle(114,offset+250,128,150,1)); //Route noire pent

            for(int i = 0; i < 3; i++)
            {
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(242,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 3; i < 7; i++)
            {
                vRoad.add(new Rectangle(198-11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(242-11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 7; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 9)
        {
            //011100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset,128,400,1)); //Route noire
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            //vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            if(Math.random() >= 0.8)
            {
                crTemp = new CollidableRectangle(114,offset+200,24,32,5,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
            }
        }
        else if(iSegmentId == 10)
        {
            //0u1100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset,44,200,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset+200,4,200,3)); //Bord à gauche
            vRoad.add(new Rectangle(154,offset,4,200,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset+200,128,200,1)); //Route noire
            vRoad.add(new Rectangle(158,offset,84,200,1)); //Route noire bis
            vRoad.add(new Rectangle(154,offset+200,4,200,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            //vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 5; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }
            for(int i = 5; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            crTemp = new CollidableRectangle(110,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(134,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 11)
        {
            //0^1100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset+100,11,300,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset+150,11,250,0,2));  //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset+200,11,200,0,2));  //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset+250,11,150,0,2));  //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(121,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(143,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(154,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            vRoad.add(new Rectangle(147,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(125,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(114,offset,11,100,1)); //Route noire pent
            vRoad.add(new Rectangle(154,offset,4,240,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
           // vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 12)
        {
            //0011^0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset+100,11,300,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset+150,11,250,0,2));  //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset+200,11,200,0,2));  //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset+250,11,150,0,2));  //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(275,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(253,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(242,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(286,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            vRoad.add(new Rectangle(242,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(253,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(264,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(275,offset,11,100,1)); //Route noire pent
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,240,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 13)
        {
            //001100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            //vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else
        {
            //Les autres, juste pour débugger à son aise
            System.out.println("Noting planned for ID : " + iSegmentId);
            System.exit(1);
            /*CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset,172,400,1)); //Route noire
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }*/
        }
    }


    /**
     * Initializes the game state (generates the road, the obstacles, the cars)
     */
    public void newGrid()
    {
        //init tick counter
        runTime = 0;
        gameRunTime = 0;
        gameMaxRunTime = 1;
       
        //Initializes finite state machine
        initFiniteStateMachine();
        FiniteState currentState = fsStates[0];

        //Re-initializes the vector of road elements and obstacles
        vTabRoad = new Vector[108];
        vTabObstacles = new Vector[109];

        //Generates 100 segments of 400x400 using the finite state machine
        for(int i = 0; i < 100; i++)
        {
            generateNextRoadSegment(vTabRoad, vTabObstacles, currentState.iId, (99-i)*400+3200);
            currentState = currentState.nextState();
        }

        //The last 8 400x400 segments are generated by taking the shortest distance to the root
        for(int i = 0; i < 8; i++)
        {
            generateNextRoadSegment(vTabRoad, vTabObstacles, currentState.iId, 3200-(i+1)*400);
            currentState = currentState.pathToRoot;
        }

        //The finish line
        CollidableRectangle crTemp = null;
        vTabRoad[3].add(crTemp = new CollidableRectangle(0,1200,400,20,2,3));
        vTabObstacles[3].add(crTemp);

        //The cars
        //Starting with the player's car
        
        //Player 1
        vCars = new Vector<Car>();
        crTemp = new Car(158,43100,32,64,6,1,0,0.5,0,0,true);
        vTabObstacles[108] = new Vector<CollidableRectangle>();
        vTabObstacles[108].add(crTemp);
        vCars.add((Car)crTemp);
        //Player 2
        crTemp = new Car(200,43100,32,64,6,1,0,0.5,0,0,true);
        //vTabObstacles[108] = new Vector<CollidableRectangle>();
        vTabObstacles[108].add(crTemp);
        vCars.add((Car)crTemp);
        //Player 3
        crTemp = new Car(242,43100,32,64,6,1,0,0.5,0,0,true);
        ///vTabObstacles[108] = new Vector<CollidableRectangle>();
        vTabObstacles[108].add(crTemp);
        vCars.add((Car)crTemp);
        //Player 4
        crTemp = new Car(110,43100,32,64,6,1,0,0.5,0,0,true);
        //vTabObstacles[108] = new Vector<CollidableRectangle>();
        vTabObstacles[108].add(crTemp);
        vCars.add((Car)crTemp);
        
        
        //Civilians
        for(int i = 0; i < 30; i++)
        {
            crTemp = new Car(204,42270-i*1000,32,64,8,1,0,0,0,1,false);
            vTabObstacles[108].add(crTemp);
            vCars.add((Car)crTemp);
        }

        //Competitors
        for(int i = 0; i < 9; i++)
        {
            crTemp = new Car(160,42200-i*900,32,64,7,1,0,0,0,2,true);
            vTabObstacles[108].add(crTemp);
            vCars.add((Car)crTemp);
        }

        //Count the number of participants (although, we should know it, but still)
        Iterator<Car> iCars = vCars.iterator();
        iCars.next(); //Skip first car
        iNbParticipants = 1;
        while(iCars.hasNext())
        {
            Car currentCar = iCars.next();
            if(currentCar.Racer)
            {
                iNbParticipants++;

            }
        }

        //Cops and speed indicator
        if(Math.random() >= 0.5)
        {
            //Cops will be placed before the speed indicator
            int pos = (int)(Math.random()*10000)+10000;
            vTabRoad[pos/400].add(crTemp = new CollidableRectangle(292,pos,30,64,11,0)); //Panneau 130
            vTabObstacles[pos/400].add(crTemp);
            vTabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292,pos-1000,30,64,14,0)); //Speedometer
            vTabObstacles[(pos-1000)/400].add(crTemp);

            pos = (int)(Math.random()*10000)+30000;
            vTabRoad[pos/400].add(crTemp = new CollidableRectangle(292,pos,30,64,11,0)); //Panneau 130
            vTabObstacles[pos/400].add(crTemp);
            vTabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292,pos-1000,32,64,12,0)); //Voiture de flics
            vTabObstacles[(pos-1000)/400].add(crTemp);
            policePos = pos-1000;
            vTabObstacles[(pos-1200)/400].add(new CollidableRectangle(0,pos-1200,400,200,13,4)); //Zone Flash

        }
        else
        {
            //Speed indicator will be placed before the cops
            int pos = (int)(Math.random()*10000)+10000;
            vTabRoad[pos/400].add(crTemp = new CollidableRectangle(292,pos,30,64,11,0)); //Panneau 130
            vTabObstacles[pos/400].add(crTemp);
            vTabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292,pos-1000,32,64,12,0)); //Voiture de flics
            vTabObstacles[(pos-1000)/400].add(crTemp);
            policePos = pos-1000;
            vTabObstacles[(pos-1200)/400].add(new CollidableRectangle(0,pos-1200,400,200,13,4)); //Zone Flash


            pos = (int)(Math.random()*10000)+30000;
            vTabRoad[pos/400].add(crTemp = new CollidableRectangle(292,pos,30,64,11,0)); //Panneau 130
            vTabObstacles[pos/400].add(crTemp);
            vTabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292,pos-1000,30,64,14,0)); //Speedometer
            vTabObstacles[(pos-1000)/400].add(crTemp);
        }

    }

    /**
     * @return the client
     */
    public int getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(int client) {
        this.client = client;
    }
    
    

}
