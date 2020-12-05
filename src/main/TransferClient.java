package main;

import java.io.*;
import java.net.*;

public class TransferClient {
    private static InetAddress host;
    private static Integer port;
    private static File file;

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("required arguments: hostname/ip port file");
        } else {
            System.out.println("*** UDP File Transfer - Client ***\n");

            // initialize variables
            host = InetAddress.getByName(args[0]);

            try {
                port = Integer.valueOf(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid port number!");
                e.printStackTrace();
                return;
            }

            file = new File(args[2]);
            if (!file.exists() || !file.isFile()) {
                System.out.println("Error: Please enter a valid file path");
                return;
            }

            // start file transfer
            FileTransfer fileTransfer = new FileTransfer();
            if (fileTransfer.sendRequest(host, port, file)) {
                System.out.println("Success: File '" + file.getName() + "' was sent successfully to host '" + host.getHostName() + "' on port " + port.toString());
            }
        }
    }
}