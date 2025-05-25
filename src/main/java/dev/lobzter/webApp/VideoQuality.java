package dev.lobzter.webApp;

public enum VideoQuality {

    LOW("worst"),
    MEDIUM("best[height<=480]"),
    HIGH("best");


    private final String format;

    VideoQuality(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
