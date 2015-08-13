package org.jlab.fastpulse;

import io.netty.buffer.ByteBuf;

import static io.netty.buffer.Unpooled.*;

/**
 * Created by john on 7/15/15.
 */
public abstract class Command {

    public static ByteBuf ReadRegisters() {

        System.out.println("ReadRegisters");

        ByteBuf buf = buffer(4);

        buf.writeShort(0x5a5a);
        buf.writeShort(0x0203);

        return buf;
    }
}
