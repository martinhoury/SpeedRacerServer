/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author martin
 */
public class SpeedRacerRMIServerImplementation extends UnicastRemoteObject implements SpeedRacerRMIServerInterface{
    Controller control;
    public SpeedRacerRMIServerImplementation () throws RemoteException{         
        super();
    }
    public SpeedRacerRMIServerImplementation (Controller control) throws RemoteException{         
        this.control = control;
    }
    @Override
    public void speedRacer() throws RemoteException {
        System.out.println("Test");
    }

    @Override
    public synchronized int register(SpeedRacerRMIClientInterface speedRacerRMIClientInterface) throws RemoteException {
        int nextIdClient = control.getNextIdClient();
        int carNumber = control.getCarAvailable().pop();
        control.getClientHashMap().put(nextIdClient, new Client(speedRacerRMIClientInterface, new Core(control,nextIdClient),nextIdClient,carNumber));
        return nextIdClient;       
    }

    @Override
    public int getScore(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().score;        
    }

    @Override
    public void newGrid(int idClient) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().newGrid();
    }

    @Override
    public boolean getGameInProgress(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().bGameInProgress;
    }

    @Override
    public boolean getGameFinishing(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().bGameFinishing;
    }

    @Override
    public boolean getGameQuit(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().bGameQuit;
    }

    @Override
    public boolean getLE_P(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().LE_P;
    }

    @Override
    public boolean getRI_P(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().RI_P;
    }

    @Override
    public boolean getUP_P(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().UP_P;
    }

    @Override
    public boolean getDO_P(int idClient) throws RemoteException {
        return control.getClientHashMap().get(idClient).getCore().DO_P;
    }

    @Override
    public void setScore(int idClient, int score) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().score = score;
    }

    @Override
    public void setGameInProgress(int idClient, boolean inProgress) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().bGameInProgress = inProgress;
    }

    @Override
    public void setGameFinishing(int idClient, boolean finishing) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().bGameFinishing = finishing;
    }

    @Override
    public void setGameQuit(int idClient, boolean quit) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().bGameQuit = quit;
    }

    @Override
    public void setLE_P(int idClient, boolean LE_P) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().LE_P =LE_P;
    }

    @Override
    public void setRI_P(int idClient, boolean RI_P) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().RI_P=RI_P;
    }

    @Override
    public void setUP_P(int idClient, boolean UP_P) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().UP_P=UP_P;
    }

    @Override
    public void setDO_P(int idClient, boolean DO_P) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().DO_P=DO_P;
    }

    @Override
    public void runGame(int idClient) throws RemoteException {
        control.getClientHashMap().get(idClient).getCore().runGame();
    }

    @Override
    public void joinGame(int idClient) throws RemoteException {
        
    }

    
}
