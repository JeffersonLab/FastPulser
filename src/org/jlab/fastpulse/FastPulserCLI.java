package org.jlab.fastpulse;

import io.netty.channel.ChannelFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

/**
 * Created by john on 7/15/15.
 */
public class FastPulserCLI {

    public static final String VERSION = "0.1";

    public static final String IP = "1.2.3.9";
    public static final int PORT = 5000;

    private Connector m_Connector;
    private Future<Device> m_ConnectFuture;


    private static Device s_Device;
    private static int s_State;

    /**
     * FastPulserCLI is designed to work only with a single device, Device reference is stored in a static
     */
    public FastPulserCLI(String addr) {
        // Test box is 1.2.3.9
        m_Connector = new Connector(addr, PORT);
    }

    public void connect() {
        m_ConnectFuture = m_Connector.connect();

        try {
            s_Device = m_ConnectFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close() {

    }


    private static void parseCommandLineArgs(String[] args) {

    }

    private static List<Device> ms_Devices;




    static void printDeviceInfo(Device d) {
        Registers regs = d.getRegisters();

        System.out.println(regs);

        System.out.println(d.getInterface(0));
    }


    static void handleState1() {
        Device d = s_Device;

        try {
            // Example of how to update registers
            // setRegister does not force a sync
            // updateRegisters writes registers, waits 250ms, then sends a request

            // Manual says this is limited to 0x88 but that isn't correct
            d.getInterface(0).setVoltage(PulserInterface.V1, 7.5f);

            //d.getInterface(0).setVoltage(PulserInterface.V2, 5.8f);
            //d.getInterface(0).setRegulator(PulserInterface.V1, 0xaa);

            d.getInterface(0).setVoltage(PulserInterface.VLED, 9.0f);
            //d.getInterface(0).setRegulator(PulserInterface.VLED, 0xcc);

            try {
                d.updateRegisters(true, 1000);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }
    }


    static void handleState2() {

        Device d = s_Device;

        Registers regs = d.getRegisters();

        try {

            if (d.getInterface(0).isEnabled()) {
                s_State = 5;

            } else {
                // Set active bit
                d.getInterface(0).setEnabled(true);

                // Wait for new registers
                s_State = 3;

                try {
                    d.updateRegisters(true, 1000);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }

            // Request a readback, data wont be valid until the next read??
            //regs.setRegisterBit(0, 9, true);
            //regs.setRegisterBit(1, 9, true);
            //regs.setRegisterBit(2, 9, true);
        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }

    }


    // Main entry point for the command line utility
    public static void main(String[] args) throws InterruptedException {

        parseCommandLineArgs(args);

        FastPulserCLI pulser = new FastPulserCLI(IP);

        pulser.connect();

        boolean shouldContinue = true;


        while (shouldContinue) {


            switch (s_State) {
                case 0:
                    System.out.println("State 0, waiting for registers...");

                    System.out.println("State 0, continuing...");
                    // Got registers

                    printDeviceInfo(s_Device);
                    s_State = 1;
                    break;

                case 1:
                    System.out.println("State 1");
                    // Update some registers
                    handleState1();
                    s_State = 2;
                    break;

                case 2:
                    System.out.println("State 2, waiting for registers...");
                    System.out.println("State 2, continuing...");

                    // If needed, set the active bit and request a readback
                    handleState2();
                    s_State = 3;
                    break;

                case 3:
                    System.out.println("State 3, waiting for registers...");
                    System.out.println("State 3, continuing...");

                    printDeviceInfo(s_Device);
                    s_State = 5;
                    break;

                case 5:
                    shouldContinue = false;
                    break;

                default:
                    // Do nothing, waiting for registers
                    break;
            }
        }


    }
}
