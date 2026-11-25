package br.edu.unipampa.varzinho.ui;

import com.formdev.flatlaf.ui.FlatButtonUI;

import javax.swing.AbstractButton;
import java.awt.Color;

/** WCAG contrast between two colours, and the focus look FlatLaf resolved for a button. */
final class Contrast {
    /** WCAG 2.2, 1.4.11: a focus indicator needs 3:1 against what surrounds it. */
    static final double FOCUS_MINIMUM = 3.0;

    private Contrast() { }

    static double ratio(Color first, Color second) {
        double lighter = Math.max(luminance(first), luminance(second));
        double darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    static Color focusedBorderColor(AbstractButton button) {
        return (Color) style(button, "focusedBorderColor");
    }

    static Color focusColor(AbstractButton button) {
        return (Color) style(button, "focusColor");
    }

    static float borderWidth(AbstractButton button) {
        return ((Number) style(button, "borderWidth")).floatValue();
    }

    static float focusWidth(AbstractButton button) {
        return ((Number) style(button, "focusWidth")).floatValue();
    }

    static Object style(AbstractButton button, String key) {
        return ((FlatButtonUI) button.getUI()).getStyleableValue(button, key);
    }

    private static double luminance(Color color) {
        return 0.2126 * channel(color.getRed()) + 0.7152 * channel(color.getGreen())
                + 0.0722 * channel(color.getBlue());
    }

    private static double channel(int value) {
        double srgb = value / 255.0;
        return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }
}
