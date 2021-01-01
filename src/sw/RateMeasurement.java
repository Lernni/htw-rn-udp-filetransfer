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

    private Timer timer;
    private String printFormat;
    private int period;
    private int sizeIncrement = 0;
    private int sizeMeasured = 0;
    private int fileSize;
    private final String UNIT_BPS = "B/s";
    private final String UNIT_BYTES = "B";

    public RateMeasurement(String printFormat, int period) {
        this.printFormat = printFormat;
        this.period = period;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public void addSize(int size) {
        this.sizeIncrement += size;
    }

    // before file request
    public void start() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentRate = (long) sizeIncrement / (period / 1000);
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

                System.out.printf((printFormat) + "%n", getRate());
            }
        }, 0, period);
    }

    // after file request
    public void stop() {
        timer.cancel();
        sizeMeasured += sizeIncrement;
        sizeIncrement = 0;
        System.out.printf((printFormat) + "%n", getResults());
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

    // source of this function: https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    public String getReadableByte(long bytes, String unit) {
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
