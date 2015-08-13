package org.jlab.fastpulse;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by john on 7/15/15.
 */
public class DeviceImpl implements Device {

    private Registers m_Registers;
    private boolean m_Connected;
    private String m_Address;
    private List<PulserInterface> m_Interface;

    private WeakReference<ChannelContext> mw_Ctx;

    DeviceImpl() {
        this("0.0.0.0");
    }

    DeviceImpl(String addr) {
        m_Connected = false;
        m_Address = addr;

        m_Registers = new Registers();
        m_Interface = new ArrayList<>();

        m_Interface.add(0, new PulserInterface(0, "J4", m_Registers,
                new byte[] {0, 0, 6, 1,     // V1 register/status/adcCount/bit
                            1, 1, 6, 0,     // V2
                            2, 2, 7, 1,     // VLED
                            10, 0,          // enable/bit
                            6, 7, 10}));    // trigger rate high/low/status

        m_Interface.add(1, new PulserInterface(1, "J3", m_Registers,
                new byte[] {3, 3, 7, 0,
                            5, 5, 8, 0,
                            4, 4, 8, 1,
                            10, 1,
                            8, 9, 11}));
    }

    public void setContext(ChannelContext ctx) {
        mw_Ctx = new WeakReference<>(ctx);
    }

    public Registers getRegisters() {

        return m_Registers;
    }

    public String getAddress() {
        return m_Address;
    }

    public PulserInterface getInterface(int id) {
        return m_Interface.get(id);
    }

    public void updateRegisters() {
        ChannelContext ctx = mw_Ctx.get();

        if (ctx != null) {
            ctx.write(m_Registers);

            /*
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            requestRegisters();
            */
        }
    }

    public void requestRegisters() {
        System.out.println("Requesting registers");

        ChannelContext ctx = mw_Ctx.get();

        if (ctx != null) {
            ctx.write(Command.ReadRegisters());
        }
    }

}
