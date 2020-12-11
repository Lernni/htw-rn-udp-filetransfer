package main;

import java.io.File;
import java.io.IOException;

public class TransferServer {

    private static int port;

    public static void main(String[] args) {
        if (args.length > 3 || args.length < 1) {
            System.out.println("required arguments: port [loss_rate avg_delay]");
        } else {
            System.out.println("*** UDP File Transfer - Server ***");

            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid port number!");
                e.printStackTrace();
                return;
            }

            // wait for file transfer
            while (true) {
                FileTransfer fileTransfer = new FileTransfer();
                try {
                    String filePath = fileTransfer.receiveRequest(port);
                    if (filePath != null) {
                        System.out.println("Success: Received '" + new File(filePath).getName() + "' !");
                    }
                } catch (IOException e) {}
            }
        }
    }
}