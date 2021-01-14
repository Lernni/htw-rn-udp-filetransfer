package sw;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Timer;
import java.util.TimerTask;

public class RateMeasurement {

    private Long minRate = null;
    private Long maxRate = null;
    private Long avgRate = null;
    private Long currentRate = null;

    private final Timer timer;
    private final String printFormat;
    private boolean timerRunning = false;
    private final int period;
    private int sizeIncrement = 0;
    private int sizeMeasured = 0;
    private long fileSize;
    private long stepDuration = 0;
    private final String UNIT_BPS = "B/s";
    private final String UNIT_BYTES = "B";

    public RateMeasurement(String printFormat, int period) {
        timer = new Timer();
        this.printFormat = printFormat;
        this.period = period;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void addSize(int size) {
        if (timerRunning) this.sizeIncrement += size;
    }

    // before file request
    public void start() {
        stepDuration = System.currentTimeMillis();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                calcRate();
                System.out.printf((printFormat) + "%n", getRate());
            }
        }, period, period);
        timerRunning = true;
    }

    // after file request
    public void stop() {
        timerRunning = false;
        timer.cancel();
        calcRate();
        System.out.printf((printFormat) + "%n", getResults());
    }

    private void calcRate() {
        stepDuration = System.currentTimeMillis() - stepDuration;

        if (stepDuration == 0) stepDuration = 1;

        currentRate = (long) (sizeIncrement / (stepDuration / 1000.0));
        sizeMeasured += sizeIncrement;
        sizeIncrement = 0;

        if (minRate == null) minRate = currentRate;
        if (maxRate == null) maxRate = currentRate;
        if (currentRate < minRate) minRate = currentRate;
        if (currentRate > maxRate) maxRate = currentRate;
        if (avgRate == null) {
            avgRate = currentRate;
        } else {
            avgRate += currentRate;
            avgRate /= 2;
        }

        stepDuration = System.currentTimeMillis();
    }

    private String getRate() {
        int progress = (int) (((double) sizeMeasured / (double) fileSize) * 100);
        return "Data rate: " + getReadableByte(currentRate, UNIT_BPS) +
                " | " + getReadableByte(sizeMeasured, UNIT_BYTES) +
                " / " + getReadableByte(fileSize, UNIT_BYTES) +
                " | " + progress + " %";
    }

    private String getResults() {
        return "Data rate: AVG: " + getReadableByte(avgRate, UNIT_BPS) +
                " | MIN: " + getReadableByte(minRate, UNIT_BPS) +
                " | MAX: " + getReadableByte(maxRate, UNIT_BPS);
    }

    // source of this function:
    // https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    public static String getReadableByte(long bytes, String unit) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " " + unit;
        }
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %c%s", bytes / 1000.0, ci.current(), unit);
    }
}
