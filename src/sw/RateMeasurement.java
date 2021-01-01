package sw;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Timer;
import java.util.TimerTask;

public class RateMeasurement {

    private int size = 0;
    private Long minRate = null;
    private Long maxRate = null;
    private Long avgRate = null;
    private Long currentRate = null;

    private Timer timer;
    private String printFormat;
    private int period;

    public RateMeasurement(String printFormat, int period) {
        this.printFormat = printFormat;
        this.period = period;
    }

    public void addSize(int size) {
        this.size += size;
    }

    // before file request
    public void start() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentRate = (long) size / (period / 1000);
                size = 0;

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
        size = 0;
        System.out.printf((printFormat) + "%n", getResults());
    }

    private String getRate() {
        return "Data rate: " + getReadableByte(currentRate);
    }

    private String getResults() {
        return "Data rate: AVG: " + getReadableByte(avgRate) +
                " | MIN: " + getReadableByte(minRate) +
                " | MAX: " + getReadableByte(maxRate);
    }

    // source of this function: https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    private String getReadableByte(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}
