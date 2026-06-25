package nv.core;

public enum ScreenSize {
    _320x240(320, 240),
    _640x480(640, 480),
    _800x600(800, 600),
    _1024x768(1024, 768),

    _1280x720(1280, 720),
    _1280x800(1280, 800),
    _1366x768(1366, 768),
    _1440x900(1440, 900),
    _1600x900(1600, 900),
    _1680x1050(1680, 1050),

    _1920x1080(1920, 1080),
    _1920x1200(1920, 1200),

    _2560x1080(2560, 1080),
    _2560x1440(2560, 1440),
    _2560x1600(2560, 1600),

    _3440x1440(3440, 1440),

    _3840x1080(3840, 1080),
    _3840x1600(3840, 1600),
    _3840x2160(3840, 2160),

    _5120x1440(5120, 1440),
    _5120x2160(5120, 2160),
    _5120x2880(5120, 2880),

    _7680x4320(7680, 4320);

    private final int width;
    private final int height;

    ScreenSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
