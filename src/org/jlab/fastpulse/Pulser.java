package org.jlab.fastpulse;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Semaphore;

/**
 * Created by john on 7/27/15.
 */
public class Pulser {
    private Module module1;
    private JPanel ParentContainer;
    private Module module2;
    private JButton applyButton;
    private JButton refreshButton;

    private FormHandler m_Handler;

    public Pulser() {
        applyButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                applyChanges();
            }
        });

        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                refreshRegisters();
            }

        });
    }

    private void runGui() {

        m_Handler = new FormHandler();

        FastPulserCLI pulser = new FastPulserCLI(FastPulserCLI.IP);

        // This needs to be able to be specified
        pulser.connect();

        boolean shouldContinue = true;

        try {

            while (shouldContinue) {

                Thread.sleep(50);

                // Wait until registers have been received for the first time
                if (m_Handler.m_Device == null)
                    continue;

                switch (m_Handler.state) {
                    case 0:
                        //We just initialized, finish setting up the form
                        getModule1().setModuleName("J4");
                        getModule2().setModuleName("J3");

                        m_Handler.state = 1;
                        break;

                    case 1:
                        // Ready for commands, wait for button press
                        break;

                    case 2:
                        // Waiting for response from last command
                        m_Handler.m_SemLock.acquire();

                        updateGui();
                        break;

                    case 3:
                        m_Handler.m_SemLock.acquire();

                        toggleEnable();
                        break;

                    default:
                        shouldContinue = false;
                        break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Module getModule1() {
        return module1;
    }

    public Module getModule2() {
        return module2;
    }


    static class FormHandler implements FastPulserHandler {

        public Device m_Device = null;
        public int state = -1;
        public Semaphore m_SemLock;

        FormHandler() {
            m_SemLock = new Semaphore(1);

            try {
                m_SemLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void initDevice(Device d) {}

        public void registersReceived(Device d) {

            System.out.println("registersReceived");

            if (m_Device == null) {
                m_Device = d;
            }

            if (state == -1) state = 0;

            m_SemLock.release();
        }
    }

    static FormHandler s_Handler;

    private void applyChanges() {

        //acquire semaphore, update registers from gui, send command
        try {
            m_Handler.m_SemLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Device d = m_Handler.m_Device;

        Module m1 = getModule1();
        Module m2 = getModule2();

        try {

            PulserInterface i1 = d.getInterface(0);
            PulserInterface i2 = d.getInterface(1);

            i1.setVoltage(PulserInterface.V1, m1.getV1Value());
            i1.setVoltage(PulserInterface.V2, m1.getV2Value());
            i1.setVoltage(PulserInterface.VLED, m1.getV3Value());

            i2.setVoltage(PulserInterface.V1, m2.getV1Value());
            i2.setVoltage(PulserInterface.V2, m2.getV2Value());
            i2.setVoltage(PulserInterface.VLED, m2.getV3Value());

            if (    (i1.isEnabled() && !m1.getEnabled()) || (!i1.isEnabled() && m1.getEnabled()) ||
                    (i2.isEnabled() && !m2.getEnabled()) || (!i2.isEnabled() && m2.getEnabled()) ) {

                m_Handler.state = 3;
            } else {
                m_Handler.state = 2;
            }

            d.updateRegisters();

        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }

        // Wait for register readback, then we update form
    }


    private void updateGui() {
        Device d = m_Handler.m_Device;

        Module m1 = getModule1();
        Module m2 = getModule2();

        try {

            PulserInterface.Status s1 = d.getInterface(0).getStatus();
            PulserInterface.Status s2 = d.getInterface(1).getStatus();

            m1.setV1Label(s1.v1_voltage);
            m1.setV2Label(s1.v2_voltage);
            m1.setV3Label(s1.vled_voltage);

            m2.setV1Label(s2.v1_voltage);
            m2.setV2Label(s2.v2_voltage);
            m2.setV3Label(s2.vled_voltage);

            m1.setEnable(s1.enable);
            m2.setEnable(s2.enable);

        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }

        // Wait for next command
        m_Handler.state = 1;
    }

    private void toggleEnable() {

        Device d = m_Handler.m_Device;

        Module m1 = getModule1();
        Module m2 = getModule2();

        try {

            d.getInterface(0).setEnabled(m1.getEnabled());
            d.getInterface(1).setEnabled(m2.getEnabled());

        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }
    }

    void refreshRegisters() {
        Device d = m_Handler.m_Device;

        d.requestRegisters();
    }


    public static void main(String[] args) {

        JFrame frame = new JFrame("Pulser");

        Pulser pulser = new Pulser();

        frame.setContentPane(pulser.ParentContainer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        pulser.runGui();
    }

}
