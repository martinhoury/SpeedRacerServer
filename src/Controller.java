/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class Controller {
    //private Vector<Client> clientHashMap; 
    private HashMap<Integer,Client> clientHashMap;
    private Stack<Integer> carAvailable;
    private int nextIdClient = 1;
    int numberOfMaxPlayers = 4;
    public Controller(){
        try {
            
            
            carAvailable = new Stack<>();
            for (int i = 0; i < numberOfMaxPlayers; i++) {
                carAvailable.push(numberOfMaxPlayers);
            }
            
            
            
            
            clientHashMap= new HashMap<>();
            //On crée l'implémentation
            SpeedRacerRMIServerImplementation speedRacerRMIImplementation = new SpeedRacerRMIServerImplementation(this);
            //On cast l'implémentation en interface pour pou
	    SpeedRacerRMIServerInterface stub = (SpeedRacerRMIServerInterface) speedRacerRMIImplementation;
            Naming.rebind("rmi://localhost:1099/SpeedRacer", stub);
            
            
            System.out.println("SpeedRacer Server is ready to listen");
            
        } catch (RemoteException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }

 


    /**
     * @return the nextIdClient
     */
    public int getNextIdClient() {
        this.nextIdClient++;
        return nextIdClient;
    }

    /**
     * @return the clientHashMap
     */
    public HashMap<Integer,Client> getClientHashMap() {
        return clientHashMap;
    }

    /**
     * @param clientHashMap the clientHashMap to set
     */
    public void setClientHashMap(HashMap<Integer,Client> clientHashMap) {
        this.clientHashMap = clientHashMap;
    }

    /**
     * @param nextIdClient the nextIdClient to set
     */
    public void setNextIdClient(int nextIdClient) {
        this.nextIdClient = nextIdClient;
    }

    /**
     * @return the carAvailable
     */
    public Stack<Integer> getCarAvailable() {
        return carAvailable;
    }

    /**
     * @param carAvailable the carAvailable to set
     */
    public void setCarAvailable(Stack<Integer> carAvailable) {
        this.carAvailable = carAvailable;
    }

}
