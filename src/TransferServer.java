import java.io.*;
import java.net.*;

public class TransferServer {
    public static void main(String[] args) throws Exception {
        if (args.length > 3 || args.length < 1) {
            System.out.println("required arguments: port [loss_rate avg_delay]");
        } else {
            System.out.println("*** UDP File Transfer - Server ***");
        }
    }
}