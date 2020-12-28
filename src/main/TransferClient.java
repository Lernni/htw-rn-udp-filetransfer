package main;

import java.io.*;
import java.net.*;

public class TransferClient {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("required arguments: hostname/ip port file");
        } else {
            System.out.println("*** UDP File Transfer - Client ***\n");

            // initialize variables
            InetAddress host = InetAddress.getByName(args[0]);

            int port;
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid port number!");
                return;
            }

            File file = new File(args[2]);
            if (!file.exists() || !file.isFile()) {
                System.out.println("Error: Please enter a valid file path");
                return;
            }

            // start file transfer
            FileTransfer fileTransfer = new FileTransfer();
            if (fileTransfer.fileRequest(host, port, file)) {
                System.out.println("Success: File '" + file.getName() + "' was sent successfully to host '" +
                        host.getHostName() + "' on port " + port + "!");
            } else {
                System.out.println("Error: File '" + file.getName() + "' could not be sent to host '" +
                        host.getHostName() + "' on port " + port + "!");
            }
        }
    }
}