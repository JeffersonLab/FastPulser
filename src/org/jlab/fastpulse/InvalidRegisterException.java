package org.jlab.fastpulse;

/**
 * Created by john on 7/15/15.
 */
public class InvalidRegisterException extends Exception {

    public InvalidRegisterException() {
        super();
    }

    public InvalidRegisterException(String reason) {
        super(reason);
    }
}
