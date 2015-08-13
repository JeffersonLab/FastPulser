package org.jlab.fastpulse;

import io.netty.buffer.ByteBuf;

import static io.netty.buffer.Unpooled.*;

/**
 * Created by john on 7/15/15.
 */
public class Registers {

    private static final int REG_SIZE = 2;

    /*
     * The FastPulser box uses the same ethernet register decoding as the EFADC, therefore
     * we must assume that is has more registers than it actually uses, and requires those
     * to be sent as dummy registers or it wil not function properly
     */

    private static final int NUM_REGISTERS = 21;
    private static final int LEN_REGISTERS = NUM_REGISTERS * REG_SIZE;
    private static final int MAX_REGISTER = 10;  // Only allow registers 0 to 10 to be used

    private static final int NUM_STATUS = 14;
    private static final int LEN_STATUS = NUM_STATUS * REG_SIZE;
    private static final int MAX_STATUS = 12;   // Only allow status 0 to 12 to be used

    static float V12_DIVISOR = 5.5f/256.0f;
    static float VLED_DIVISOR = 1.0f/32.0f;

    private ByteBuf m_Registers;
    private ByteBuf m_Status;

    Registers() {
        m_Registers = buffer(LEN_REGISTERS);
        //m_Registers.markReaderIndex();

        m_Status = buffer(LEN_STATUS);
        m_Status.markReaderIndex();
    }


    void decode(ByteBuf buf) {

        // ** These methods do not alter the reader or writer indices of the target buffers
        //    The readerIndex of the source 'buf' does get incremented

        m_Registers.setBytes(0, buf, LEN_REGISTERS);

        m_Status.setBytes(0, buf, LEN_STATUS);
    }


    byte[] encode() {
        // Set readerIndex just in case
        //m_Registers.resetReaderIndex();

        return m_Registers.array();
    }


    public int getRegister(int reg) throws InvalidRegisterException {
        if (reg < 0 || reg > MAX_REGISTER) {
            throw new InvalidRegisterException();
        }

        int offset = reg * REG_SIZE;

        int value = m_Registers.getUnsignedShort(offset);

        return value;
    }

    public int getStatusRegister(int reg) throws InvalidRegisterException {
        if (reg < 0 || reg > MAX_STATUS)
            throw new InvalidRegisterException();

        return m_Status.getUnsignedShort(reg * REG_SIZE);
    }


    public void setRegister(int reg, short val) throws InvalidRegisterException {

        if (reg < 0 || reg > MAX_REGISTER) {
            throw new InvalidRegisterException();
        }

        int offset = reg * REG_SIZE;

        m_Registers.setShort(offset, val);
    }

    public int setRegisterBit(int reg, int bit, boolean val) throws InvalidRegisterException {
        int word = getRegister(reg);

        if (val) {
            word |= (1 << bit);
        } else {
            word &= ~(1 << bit);
        }

        setRegister(reg, (short) word);

        return word;
    }

    public boolean isSet(int reg, int bit) {
        return (((reg >> bit) & 0x1) == 1);
    }


    /*
    public int getTriggerRate(int id) throws InvalidRegisterException {
        if (id < 0 || id > 1)
            throw new InvalidRegisterException();

        int offset = (id == 0 ? 6 : 8);

        int r1 = getRegister(offset);
        int r2 = getRegister(offset + 1);

        int rate = r1 | ((r2 & 0x3800) << 5);

        return rate;
    }
    */


    /**
     *
     * @param id Pulser Id, 1 for J4, 2 for J3
     * @return Frequency in Hz
     * @throws InvalidRegisterException
     */
    public int getTriggerFrequency(int id) throws InvalidRegisterException {
        if (id < 0 || id > 1)
            throw new InvalidRegisterException();

        // frequency is in 10's of Hz
        int freq = getStatusRegister(id == 0 ? 10 : 11) * 10;

        return freq;
    }

    public int getFirmwareVersion() {
        int s9 = 0;

        try {
            s9 = getStatusRegister(9);
        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }

        return s9 & 0x1f;
    }

    /**
        02 5a 02 46 02 4b 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01
       [00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 00 80 00 80 00]

     025a 0246 024b 0000 0000 0000 0000 0000 0000 0000 0001 [0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 8000 8000 8000]
    */

    public String toString() {
        StringBuilder strB = new StringBuilder();

        for (int i = 0; i < NUM_REGISTERS; i++) {
            strB.append(String.format("%04x ", m_Registers.getUnsignedShort(i*2)));
        }

        strB.append("[");
        for (int i = 0; i < NUM_STATUS; i++) {
            strB.append(String.format("%04x", m_Status.getUnsignedShort(i*2)));
            if (i < NUM_STATUS- 1)
                strB.append(" ");
        }
        strB.append("]");

        return strB.toString();
    }

}
