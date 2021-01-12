package sw;

public class RTOCalc {

    // RTO calculation after RFC 6298 like TCP
    // for client timeout
    // without clock resolution

    private final double alpha = 0.125;
    private final double beta = 0.25;

    private int RTO;
    private long RTT;
    private double sRTT = 0;
    private double RTTVAR;

    public RTOCalc(int startRTO) {
        RTO = startRTO;
    }

    public void startTime() {
        RTT = System.currentTimeMillis();
    }

    public void stopTime() {
        RTT = System.currentTimeMillis() - RTT;

        if (sRTT == 0.0) {
            sRTT = RTT;
            RTTVAR = (double) (RTT / 2);
        } else {
            RTTVAR = (1 - beta) * RTTVAR + beta * Math.abs(sRTT - RTT);
            sRTT = (1 - alpha) * sRTT + alpha * RTT;
        }
        RTO = (int) (sRTT + 4 * RTTVAR);
    }

    public int getRTO() {
        return RTO;
    }
}
