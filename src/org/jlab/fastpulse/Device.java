package org.jlab.fastpulse;

import java.util.concurrent.TimeoutException;

/**
 * Created by john on 7/15/15.
 */
public interface Device {
    String getAddress();

    Registers getRegisters();

    PulserInterface getInterface(int id);

    DeviceFuture updateRegisters(boolean wait, int timeout) throws TimeoutException;

    DeviceFuture requestRegisters(boolean wait, int timeout) throws TimeoutException;
}
