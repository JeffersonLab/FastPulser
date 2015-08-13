package org.jlab.fastpulse;

/**
 * Created by john on 7/17/15.
 */


import java.lang.ref.WeakReference;

/**
 * PulserInterface is a view into the register map related to a single pulser device in the box (J3 and J4)
 *
 */
public class PulserInterface {

    // offsets
    private static final byte V1_REGISTER = 0;
    private static final byte V1_STATUS = 1;
    private static final byte V1_ADC_REG = 2;
    private static final byte V1_ADC_BIT = 3;
    private static final byte V2_REGISTER = 4;
    private static final byte V2_STATUS = 5;
    private static final byte V2_ADC_REG = 6;
    private static final byte V2_ADC_BIT = 7;
    private static final byte V3_REGISTER = 8;
    private static final byte V3_STATUS = 9;
    private static final byte V3_ADC_REG = 10;
    private static final byte V3_ADC_BIT = 11;
    private static final byte ENABLE_REGISTER = 12;
    private static final byte ENABLE_BIT = 13;
    private static final byte TRIG_RATE_REG_LOW = 14;
    private static final byte TRIG_RATE_REG_HIGH = 15;
    private static final byte TRIG_RATE_STATUS = 16;

    private static final float[] s_VRef = new float[] {3.126f, 3.126f, 3.126f};
    private static final float[] s_PotStep = new float[] {19.53125f, 19.53125f, 39.0525f};

    public static final byte V1 = 0;
    public static final byte V2 = 1;
    public static final byte VLED = 2;

    public static final byte VALID_BIT = 14;
    public static final byte READY_BIT = 15;
    public static final byte TRIG_ENABLE_BIT = 14;
    public static final byte TRIG_SOURCE_BIT = 15;

    enum TrigSource {
        TRIG_RATE(false),
        TRIG_IN(true);

        public boolean value;

        TrigSource(boolean val) {
            this.value = val;
        }
    }

    public static final String[] regId = new String[] {"1", "2", "LED"};

    private int m_Id;
    private String m_Name;
    private byte[] m_Offsets;
    private Status m_Status;
    private WeakReference<Registers> mw_Registers;


    class Status {
        public short v1_value, v2_value, vled_value;
        public boolean v1_valid, v2_valid, vled_valid;
        public boolean v1_ready, v2_ready, vled_ready;
        public short v1_adcCount, v2_adcCount, vled_adcCount;
        public float v1_voltage, v2_voltage, vled_voltage;
        public short trigRate;
        public boolean enable, trigEnable;

        public void decode(Registers r) throws InvalidRegisterException {

            // V1
            int v1 = r.getStatusRegister(m_Offsets[V1_STATUS]);

            v1_value = (short)(v1 & 0x1FF);
            v1_valid = r.isSet(v1, VALID_BIT);
            v1_ready = r.isSet(v1, READY_BIT);

            // V2
            int v2 = r.getStatusRegister(m_Offsets[V2_STATUS]);

            v2_value = (short)(v2 & 0x1FF);
            v2_valid = r.isSet(v2, VALID_BIT);
            v2_ready = r.isSet(v2, READY_BIT);

            // VLED
            int v3 = r.getStatusRegister(m_Offsets[V3_STATUS]);

            vled_value = (short)(v3 & 0x1FF);
            vled_valid = r.isSet(v3, VALID_BIT);
            vled_ready = r.isSet(v3, READY_BIT);

            // V1 ADC count
            int v1c = r.getStatusRegister(m_Offsets[V1_ADC_REG]);
            v1_adcCount = (short)(v1c >> (8 * m_Offsets[V1_ADC_BIT]) & 0x00ff);   // This just shifts if adcCount was in the high bits

            int v2c = r.getStatusRegister(m_Offsets[V2_ADC_REG]);
            v2_adcCount = (short)(v2c >> (8 * m_Offsets[V2_ADC_BIT]) & 0x00ff);

            int v3c = r.getStatusRegister(m_Offsets[V3_ADC_REG]);
            vled_adcCount = (short)(v3c >> (8 * m_Offsets[V3_ADC_BIT]) & 0x00ff);

            // Convert adcCount to voltages
            v1_voltage = v1_adcCount * Registers.V12_DIVISOR;
            v2_voltage = v2_adcCount * Registers.V12_DIVISOR;
            vled_voltage = vled_adcCount * Registers.VLED_DIVISOR;

            // Multiply status reg by 10 to get Hz
            trigRate = (short)(r.getStatusRegister(m_Offsets[TRIG_RATE_STATUS]) * 10);


            // note that ENABLE_BIT is accessed thru the offsets, it shouldn't need to be as the same bit is
            // used in both modules
            enable = r.isSet(m_Offsets[ENABLE_REGISTER], m_Offsets[ENABLE_BIT]);
            trigEnable = r.isSet(m_Offsets[TRIG_RATE_REG_HIGH], TRIG_ENABLE_BIT);
        }
    }

    // Id 1 is pulser 1, or J4
    // Id 2 is pulser 2, or J3 (backwards??)
    PulserInterface(int id, String name, Registers regs, byte[] offsets) {
        m_Id = id;
        m_Name = name;
        m_Offsets = offsets;

        m_Status = new Status();

        mw_Registers = new WeakReference<>(regs);
    }

    private Registers getRegisters() throws InvalidRegisterException {
        Registers r = mw_Registers.get();

        if (r == null)
            throw new InvalidRegisterException("Bad reference to Registers object");

        return r;
    }


    public Status getStatus() throws InvalidRegisterException {

        m_Status.decode(getRegisters());

        return m_Status;
    }


    public void setEnabled(boolean val) throws InvalidRegisterException {

        getRegisters().setRegisterBit(m_Offsets[ENABLE_REGISTER], m_Offsets[ENABLE_BIT], val);
    }

    public boolean isEnabled() throws InvalidRegisterException {
        return getRegisters().isSet(m_Offsets[ENABLE_REGISTER], m_Offsets[ENABLE_BIT]);
    }


    public void setRegulator(int regulator, int value) throws InvalidRegisterException {

        if (regulator < V1 || regulator > VLED)
            throw new InvalidRegisterException("Invalid Regulator");

        Registers r = getRegisters();

        int register = m_Offsets[regulator == V1 ? V1_REGISTER : (regulator == V2 ? V2_REGISTER : V3_REGISTER)];

        System.out.printf("Setting %s[%d] V%s register(%d) to %d\n", m_Name, m_Id, regId[regulator], register, value);

        r.setRegister(register, (short)value);
        r.setRegisterBit(register, 9, false);   // Make sure write bit is 0
    }


    public void setVoltage(int regulator, float volts) throws InvalidRegisterException {

        if (regulator < V1 || regulator > VLED)
            throw new InvalidRegisterException("Invalid Regulator");

        // This does not seem to be the correct formula, it gets very inaccurate around 5+ volts
        float R3 = (volts - s_VRef[regulator]) / .00089333f;

        double vv = R3 / s_PotStep[regulator];

        short vreg = (short)(vv + 0.5);

        System.out.printf("Setting %s[%d] V%s voltage to %.3f, R3 = %.3f, reg = %d\n", m_Name, m_Id, regId[regulator], volts, R3, vreg);

        setRegulator(regulator, vreg);
    }


    /**
     * From Hai's VHDL:
     * Pulse1_Rate          <= (CONFIG7(13 downto 11) & CONFIG6) ;
     * Pulse2_Rate          <= (CONFIG9(13 downto 11) & CONFIG8) ;
     * @param rate Rate in Hz
     * @throws InvalidRegisterException
     */
    public void setTriggerRate(int rate) throws InvalidRegisterException {

        Registers r = getRegisters();

        int rlow = m_Offsets[TRIG_RATE_REG_LOW];    // config 6 or 8
        int rhigh = m_Offsets[TRIG_RATE_REG_HIGH];  // config 7 or 9

        int reg_low_val = rate & 0x0000ffff;

        // mask to save bits 15:14 from register, OR with high bits of rate shifted to bits 13:11
        int reg_high_val = (r.getRegister(rhigh) & 0xc000) | ((rate & 0x70000) >> 5);

        r.setRegister(rlow, (short) reg_low_val);
        r.setRegister(rhigh, (short) reg_high_val);
    }


    public void setTriggerEnable(boolean val) throws InvalidRegisterException {

        Registers r = getRegisters();

        int reg = m_Offsets[TRIG_RATE_REG_HIGH];

        r.setRegisterBit(reg, TRIG_ENABLE_BIT, val);
    }


    public void setTriggerSource(TrigSource source) throws InvalidRegisterException {

        Registers r = getRegisters();

        int reg = m_Offsets[TRIG_RATE_REG_HIGH];

        r.setRegisterBit(reg, TRIG_SOURCE_BIT, source.value);
    }


    public String toString() {
        StringBuilder strB = new StringBuilder();

        // decode status from latest registers
        try {
            getStatus();
        } catch (InvalidRegisterException e) {
            e.printStackTrace();
        }

        strB.append(String.format("V1 Voltage: %.2f  [Register: %d (%svalid) (%sready)]\n",
                m_Status.v1_voltage, m_Status.v1_value, (m_Status.v1_valid ? "" : "!"), (m_Status.v1_ready ? "":"!")));

        strB.append(String.format("V2 Voltage: %.2f  [Register: %d (%svalid) (%sready)]\n",
                m_Status.v2_voltage, m_Status.v2_value, (m_Status.v2_valid ? "" : "!"), (m_Status.v2_ready ? "":"!")));

        strB.append(String.format("V3 Voltage: %.2f  [Register: %d (%svalid) (%sready)]\n",
                m_Status.vled_voltage, m_Status.vled_value, (m_Status.vled_valid ? "" : "!"), (m_Status.vled_ready ? "":"!")));

        strB.append(String.format("Trigger Rate: %d Hz\n", m_Status.trigRate));

        return strB.toString();
    }
}
