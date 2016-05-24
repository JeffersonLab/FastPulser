package org.jlab.fastpulse;

import io.netty.util.Timeout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Created by john on 7/15/15.
 */
public class DeviceImpl implements Device {

    private Registers m_Registers;
    private boolean m_Connected;
    private String m_Address;
    private List<PulserInterface> m_Interface;

    private WeakReference<ChannelContext> mw_Ctx;

    private DeviceFuture m_DeviceFuture = null;

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
                new byte[]{3, 3, 7, 0,
                        5, 5, 8, 0,
                        4, 4, 8, 1,
                        10, 1,
                        8, 9, 11}));
    }

    DeviceFuture getDeviceFuture() {
        return m_DeviceFuture;
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

    /**
     * TODO: This should block until the new register set is received
     */
    public DeviceFuture updateRegisters(boolean wait, int timeout) throws TimeoutException {
        ChannelContext ctx = mw_Ctx.get();

        if (ctx != null && m_DeviceFuture == null) {
            ctx.write(m_Registers);

            m_DeviceFuture = new DeviceFuture();

            if (wait) {
                try {
                    m_DeviceFuture.get(timeout, TimeUnit.MILLISECONDS);

                } catch (ExecutionException | InterruptedException e) {e.printStackTrace();}
            }
        }

        return m_DeviceFuture;
    }

    /**
     * TODO: This should block until the register set is received
     */
    public DeviceFuture requestRegisters(boolean wait, int timeout) throws TimeoutException {
        Logger.getLogger("global").info("Requesting registers");

        ChannelContext ctx = mw_Ctx.get();

        if (ctx != null && m_DeviceFuture == null) {
            ctx.write(Command.ReadRegisters());

            m_DeviceFuture = new DeviceFuture();

            if (wait) {
                try {
                    m_DeviceFuture.get(timeout, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | InterruptedException e) {e.printStackTrace();}
            }
        }

        return m_DeviceFuture;
    }

}
