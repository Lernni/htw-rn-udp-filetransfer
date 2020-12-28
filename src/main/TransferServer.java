package main;

import java.io.File;
import java.io.IOException;

public class TransferServer {
    public static void main(String[] args) {
        if (args.length > 3 || args.length < 1) {
            System.out.println("required arguments: port [loss_rate avg_delay]");
        } else {
            System.out.println("*** UDP File Transfer - Server ***");

            int port;
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
                    String filePath = fileTransfer.fileIndication(port);
                    if (filePath != null) {
                        System.out.println("Success: Received file '" + new File(filePath).getName() +
                                "' on port " + port + "!");
                    } else {
                        System.out.println("Error: Could not receive a file on port " + port + "!");
                    }
                } catch (IOException ignored) {}
            }
        }
    }
}