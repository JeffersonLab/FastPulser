package org.jlab.fastpulse;


import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Created by john on 7/27/15.
 */
public class Pulser {
    private Module module1;
    private JPanel ParentContainer;
    private Module module2;
    private JButton applyButton;
    private JButton refreshButton;

    private Connector m_Connector;
    private Device m_Device;

    public Pulser() {
        applyButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                applyChanges();
                updateGui();
            }
        });

        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                refreshRegisters();
                updateGui();
            }

        });

    }

    private void runGui() {

        if (m_Connector == null) {

            // TODO Move this to connect button handler
            m_Connector = new Connector(FastPulserCLI.IP, FastPulserCLI.PORT);

            Future<Device> cf = m_Connector.connect();

            try {
                m_Device = cf.get(1000, TimeUnit.MILLISECONDS);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                Logger.getLogger("global").severe("Connection attempt timeout");
                return;
            }

            getModule1().setModuleName("J4");
            getModule2().setModuleName("J3");

        }

    }

    public Module getModule1() {
        return module1;
    }

    public Module getModule2() {
        return module2;
    }


    private void applyChanges() {

        Device d = m_Device;

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

            // I think this is taken care of in updateGui()?
            /*
            if (    (i1.isEnabled() && !m1.getEnabled()) || (!i1.isEnabled() && m1.getEnabled()) ||
                    (i2.isEnabled() && !m2.getEnabled()) || (!i2.isEnabled() && m2.getEnabled()) ) {

            }
            */


            try {
                d.updateRegisters(true, 1000);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }
        
    }


    private void updateGui() {
        Device d = m_Device;

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

            d.getInterface(0).setEnabled(m1.getEnabled());
            d.getInterface(1).setEnabled(m2.getEnabled());

        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }

    }


    void refreshRegisters() {
        Device d = m_Device;

        try {
            d.requestRegisters(true, 1000);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
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
