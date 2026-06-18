package me.eigenraven.personalspace.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Function;
import java.util.function.IntConsumer;

public class IntSliderButton extends AbstractSliderButton {
    private final int minValue;
    private final int maxValue;
    private final Function<Integer, Component> messageFactory;
    private final IntConsumer onChanged;

    private int intValue;

    public IntSliderButton(
            int x,
            int y,
            int width,
            int height,
            int minValue,
            int maxValue,
            int initialValue,
            Function<Integer, Component> messageFactory,
            IntConsumer onChanged
    ) {
        super(
                x,
                y,
                width,
                height,
                Component.empty(),
                toSliderValue(minValue, maxValue, initialValue)
        );

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.messageFactory = messageFactory;
        this.onChanged = onChanged;
        this.intValue = clamp(initialValue, minValue, maxValue);

        updateMessage();
    }

    public int getIntValue() {
        return intValue;
    }

    @Override
    protected void updateMessage() {
        setMessage(messageFactory.apply(intValue));
    }

    @Override
    protected void applyValue() {
        intValue = minValue + (int) Math.round(value * (maxValue - minValue));
        intValue = clamp(intValue, minValue, maxValue);

        onChanged.accept(intValue);
        updateMessage();
    }

    private static double toSliderValue(int minValue, int maxValue, int value) {
        if (maxValue <= minValue) {
            return 0.0D;
        }

        int clamped = clamp(value, minValue, maxValue);
        return (clamped - minValue) / (double) (maxValue - minValue);
    }

    private static int clamp(int value, int minValue, int maxValue) {
        return Mth.clamp(value, minValue, maxValue);
    }
}