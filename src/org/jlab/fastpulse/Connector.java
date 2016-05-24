package org.jlab.fastpulse;

import io.netty.channel.EventLoopGroup;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Created by john on 7/15/15.
 */
public class Connector {

    private EventLoopGroup m_Group;

    private int m_Port;
    private String m_Address;
    private ConnectFuture m_ConnectFuture;
    private ConnectState m_ConnectState;

    enum ConnectState {DISCONNECTED, CONNECTING, CONNECTED}
    private enum State {WAITING, DONE, CANCELLED}

    /**
     * Main device handler, manages access to the device via semaphore which is acquired and released
     * after each command and receipt of new registers (automatically sent by the device)
     */
    private abstract class FutureHandler implements FastPulserHandler {
        public Semaphore m_SemLock;
        protected DeviceImpl m_Device;
        private String m_Address;

        FutureHandler(String address) {
            m_Address = address;
            m_SemLock = new Semaphore(1);

            try {
                m_SemLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        public String getAddress() {
            return m_Address;
        }

        @Override
        @Deprecated
        public void initDevice(Device d) {}

        @Override
        public void registersReceived(Device d) {
            m_SemLock.release();

            // Complete the future of any pending command which may have prompted the registers
            DeviceFuture df = ((DeviceImpl)d).getDeviceFuture();

            if (df != null) {
                df.complete(d.getRegisters());
            }
        }

        public DeviceImpl getDevice() { return m_Device; }

        public void initDevice() { m_Device = new DeviceImpl(); }
    }

    class ConnectFuture extends FutureHandler implements Future<Device> {

        private volatile State state = State.WAITING;
        private final BlockingQueue<Device> reply = new ArrayBlockingQueue<>(1);
        Registers lastSet = null;

        ConnectFuture(String addr) {
            super(addr);
        }

        /**
         * We complete the future with the parameter reference from registersReceived even though we retain
         * a reference to the DeviceImpl at an earlier stage.
         * DeviceImpl is instantiated in the ChannelContext upon receipt of packet from an ip address who does
         * not have a corresponding entry in the devices table
         * @param d
         */
        @Override
        public void registersReceived(Device d) {

            Logger.getLogger("global").info("in ConnectFuture registersReceived()");

            Registers r = d.getRegisters();

            super.registersReceived(d);

            lastSet = r;

            if (m_ConnectState == ConnectState.CONNECTING) {
                m_ConnectState = ConnectState.CONNECTED;

                Logger.getLogger("global").info("ConnectHandler.registersReceived(), state CONNECTED");

                try {
                    reply.put(d);
                    state = State.DONE;
                } catch (InterruptedException e) {
                    state = State.CANCELLED;
                }
            }
        }


        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            Logger.getLogger("global").info("ConnectHandler.cancel()");

            state = State.CANCELLED;

            return true;
        }

        @Override
        public boolean isCancelled() { return state == State.CANCELLED; }

        @Override
        public boolean isDone() { return state == State.DONE; }

        @Override
        public Device get() throws InterruptedException, ExecutionException {
            Logger.getLogger("global").info("ConnectHandler.get()");

            return this.reply.take();
        }

        @Override
        public Device get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            Logger.getLogger("global").info(String.format("ConnectHandler.get(%d)", timeout));

            final Device replyOrNull = reply.poll(timeout, unit);
            if (replyOrNull == null) {
                throw new TimeoutException();
            }
            return replyOrNull;
        }

    }

    Connector(String address, int port) {
        m_Port = port;
        m_Address = address;

        m_ConnectState = ConnectState.DISCONNECTED;

        Logger.getLogger("global").info("new Connector, state DISCONNECTED");

    }


    public Future<Device> connect() {
        Logger.getLogger("global").info("Connecting...");

        //m_Client.setDebug(debug);

        // Set up temporary ClientHandler to detect connection progress
        m_ConnectFuture = new ConnectFuture(m_Address);

        Logger.getLogger("global").info(" => connect()");


        // This initiates a register request
        SocketListener.instance().connect(m_ConnectFuture);


        m_ConnectState = ConnectState.CONNECTING;
        Logger.getLogger("global").info(" => ReadRegisters() :: state CONNECTING");


		/*
		// We're asynchronous so we could possibly get here after the connect sequence when the timer should be stopped/null
		if (m_ConnectState != ConnectState.CONNECTED && m_ConnectTimeout != null)
			m_ConnectTimeout.start();
		*/

        return m_ConnectFuture;
    }


    public void shutdown() {
        m_Group.shutdownGracefully();
    }
}
