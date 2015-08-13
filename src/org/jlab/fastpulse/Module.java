package org.jlab.fastpulse;

import javax.swing.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by john on 7/23/15.
 */
public class Module {
    private JCheckBox chkEnable;
    private JLabel tfModuleName;
    private JFormattedTextField tfV1;
    private JFormattedTextField tfV2;
    private JFormattedTextField tfVLED;
    private JLabel lblV1Value;
    private JLabel lblV2Value;
    private JLabel lblV3Value;
    private JPanel ParentContainer;
    private JFormattedTextField tfTrigRate;

    public void setModuleName(String value) {
        tfModuleName.setText(value);
    }

    public boolean getEnabled() {
        return chkEnable.isEnabled();
    }

    public void setEnable(boolean value) {
        chkEnable.setSelected(value);
    }

    public float getV1Value() {
        return Float.parseFloat(tfV1.getText());
    }

    public float getV2Value() {
        return Float.parseFloat(tfV2.getText());
    }

    public float getV3Value() {
        return Float.parseFloat(tfVLED.getText());
    }

    public void setV1Label(float value) {
        lblV1Value.setText(String.format("%.2f V", value));
    }

    public void setV2Label(float value) {
        lblV2Value.setText(String.format("%.2f V", value));
    }

    public void setV3Label(float value) {
        lblV3Value.setText(String.format("%.2f V", value));
    }

    private void createUIComponents() {

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
        decimalFormat.setGroupingUsed(false);

        tfV1 = new JFormattedTextField(decimalFormat);
        tfV1.setColumns(3);

        tfV2 = new JFormattedTextField(decimalFormat);
        tfV2.setColumns(3);

        tfVLED = new JFormattedTextField(decimalFormat);
        tfVLED.setColumns(3);

        tfTrigRate = new JFormattedTextField(NumberFormat.getIntegerInstance(Locale.getDefault()));
        tfTrigRate.setColumns(5);
    }
}



