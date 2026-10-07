package de.dresdenairlines;

public final class Livery {
    public enum Pattern {
        CLASSIC,
        MONOCHROME,
        TWO_TONE,
        TAIL_ACCENT
    }

    public int primary = 0xFFFFFF;
    public int secondary = 0xFFFFFF;
    public int accent = 0xFFFFFF;
    public int tail = 0xFFFFFF;
    public Pattern pattern = Pattern.CLASSIC;

    public Livery() {}

    public Livery(int primary, int secondary, int accent) {
        this.primary = primary;
        this.secondary = secondary;
        this.accent = accent;
        this.tail = secondary;
    }

    public Livery(int primary, int secondary, int accent, int tail, Pattern pattern) {
        this.primary = primary;
        this.secondary = secondary;
        this.accent = accent;
        this.tail = tail;
        this.pattern = pattern == null ? Pattern.CLASSIC : pattern;
    }

    public int[] modelColors() {
        return switch (pattern) {
            case MONOCHROME -> new int[]{primary, primary, primary, primary};
            case TWO_TONE -> new int[]{primary, secondary, accent, secondary};
            case TAIL_ACCENT -> new int[]{primary, secondary, accent, tail};
            case CLASSIC -> new int[]{primary, secondary, accent, tail};
        };
    }
}
