package org.jlab.fastpulse;

/**
 * Created by john on 7/15/15.
 */
public interface Device {
    String getAddress();

    Registers getRegisters();

    PulserInterface getInterface(int id);

    void updateRegisters();

    void requestRegisters();
}
