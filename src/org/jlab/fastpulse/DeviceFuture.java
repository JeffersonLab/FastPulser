package org.jlab.fastpulse;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Represents the future of a device action, typically a request for registers
 *
 * Created by john on 8/19/15.
 */
public class DeviceFuture implements Future<Registers> {

    private enum State {WAITING, DONE, CANCELLED}

    private volatile State state = State.WAITING;
    private final BlockingQueue<Registers> reply = new ArrayBlockingQueue<>(1);


    public void complete(Registers regs) {

        Logger.getLogger("global").info("in DeviceFuture complete()");

        try {
            reply.put(regs);
            state = State.DONE;
        } catch (InterruptedException e) {
            state = State.CANCELLED;
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
    public Registers get() throws InterruptedException, ExecutionException {
        Logger.getLogger("global").info("DeviceFuture.get()");

        return this.reply.take();
    }

    @Override
    public Registers get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Logger.getLogger("global").info(String.format("DeviceFuture.get(%d)", timeout));

        final Registers replyOrNull = reply.poll(timeout, unit);
        if (replyOrNull == null) {
            throw new TimeoutException();
        }
        return replyOrNull;
    }

}
