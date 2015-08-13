package org.jlab.fastpulse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by john on 7/15/15.
 */
class ChannelContext {

    private WeakReference<ChannelHandlerContext> mw_Ctx;


    ChannelContext(final ChannelHandlerContext ctx) {
        mw_Ctx = new WeakReference<>(ctx);
    }


    final ChannelHandlerContext getContext() {
        return mw_Ctx.get();
    }


    void write(Registers regs) {
        ChannelHandlerContext ctx = mw_Ctx.get();

        if (ctx == null)
            return;

        ctx.writeAndFlush(regs);
    }

    void write(ByteBuf buf) {
        ChannelHandlerContext ctx = mw_Ctx.get();

        if (ctx == null)
            return;

        ctx.writeAndFlush(buf);
    }


    Connector.ConnectFuture getHandler(SocketAddress addr) {
        Connector.ConnectFuture d = null;

        String strAddr = addr.toString();

        SocketListener li = SocketListener.instance();

        for (Connector.ConnectFuture di : li.m_ConnectionList) {

            // Convert address to /0.0.0.0:0000 format to match 'sender' from SocketAddress
            if (strAddr.equals("/"+di.getAddress()+":"+FastPulserCLI.PORT)) {
                d = di;
                break;
            }
        }

        return d;
    }

    void parseToDevice(ByteBuf buf, SocketAddress addr) {

        // Find the handler associated with address
        Connector.ConnectFuture cf = getHandler(addr);

        if (cf == null) {
            System.out.println("Could not find device handler with address " + addr);
            return;
        }

        if (cf.getDevice() == null)
            cf.initDevice();    // Not to be confused with the interface method initDevice(d)

        // Sync the register values with the device
        cf.getDevice().getRegisters().decode(buf);


        // Push registersReceived event out to the handler
        // The handler here IS the connectfuture
        cf.registersReceived(cf.getDevice());
    }


}

