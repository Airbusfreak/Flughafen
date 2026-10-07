package de.dresdenairlines;

public final class Livery {
    public int primary = 0xFFFFFF;
    public int secondary = 0xFFFFFF;
    public int accent = 0xFFFFFF;

    public Livery() {}

    public Livery(int primary, int secondary, int accent) {
        this.primary = primary;
        this.secondary = secondary;
        this.accent = accent;
    }
}
