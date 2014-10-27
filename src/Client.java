/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author martin
 */
public class Client {
    private SpeedRacerRMIClientInterface callback;
    private Core core;
    private int id;
    private int carNumber;
    private boolean connected;
    public Client(SpeedRacerRMIClientInterface callback,Core core,int id,int carNumber){
        this.callback = callback;
        this.core = core;
        this.id=id;
    }
    /**
     * @return the callback
     */
    public SpeedRacerRMIClientInterface getCallback() {
        return callback;
    }

    /**
     * @param callback the callback to set
     */
    public void setCallback(SpeedRacerRMIClientInterface callback) {
        this.callback = callback;
    }

    /**
     * @return the core
     */
    public Core getCore() {
        return core;
    }

    /**
     * @param core the core to set
     */
    public void setCore(Core core) {
        this.core = core;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * @param connected the connected to set
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    
}
