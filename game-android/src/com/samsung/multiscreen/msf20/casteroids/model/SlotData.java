package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Contains a slot's availability and color data.
 * 
 * @author Dan McCafferty
 * 
 */
public class SlotData implements Comparable<SlotData> {
    // A flag indicating whether or not this slot is available.
    private final boolean available;

    // The color associated to the slot.
    private final Color color;

    /**
     * Constructor.
     * 
     * @param available
     *            A flag indicating whether or not this slot is available.
     * @param color
     *            The color associated to the slot.
     */
    public SlotData(boolean available, Color color) {
        super();
        this.available = available;
        this.color = color;
    }

    /**
     * Returns a flag indicating whether or not this slot is available.
     * 
     * @return
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Returns the color associated to the slot.
     * 
     * @return
     */
    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "SlotData [available=" + available + ", color=" + color + "]";
    }

    @Override
    public int compareTo(SlotData another) {
        // Sort the slot by the color's ordinal so that they are in the order the Colors are defined.
        return (another.getColor().ordinal() - getColor().ordinal());
    }
}
