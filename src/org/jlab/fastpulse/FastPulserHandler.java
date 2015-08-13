package org.jlab.fastpulse;

/**
 * Created by john on 7/15/15.
 */
public interface FastPulserHandler {

    void initDevice(Device d);

    void registersReceived(Device d);
    
}
