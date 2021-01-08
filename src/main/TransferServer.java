package main;

import sw.RateMeasurement;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class TransferServer {
    public static void main(String[] args) throws Exception {
        final String requiredArgs = "required arguments: port [loss_rate avg_delay] ['debug']";

        if (args.length > 4 || args.length < 1) {
            System.out.println(requiredArgs);
        } else {
            System.out.println("*** UDP File Transfer - Server ***");

            // initialize variables
            int port;
            int averageDelay = 0;
            double lossRate = 0.0;
            boolean debugMode = false;

            try {
                port = Integer.parseInt(args[0]);

                // check for debug mode
                if (args.length == 2) debugMode = args[1].equals("debug");
                if (args.length == 4) debugMode = args[3].equals("debug");

                // check for loss rate and average delay
                if (args.length >= 3) {
                    lossRate = Double.parseDouble(args[1]);
                    averageDelay = Integer.parseInt(args[2]);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number!\n" +
                        "port/avg_delay as Integer, loss_rate as Double\n" + requiredArgs);
                return;
            }

            // print server info
            InetAddress server = InetAddress.getLocalHost();
            System.out.println("Server Info:\n" +
                    "Host Name: " + server.getHostName() + "\n" +
                    "IP Address: " + server.getHostAddress() + "\n" +
                    "Port: " + port + "\n");

            // wait for file transfer
            while (true) {

                System.out.println("**********************************");

                FileTransfer fileTransfer = new FileTransfer(lossRate, averageDelay, debugMode);
                try {
                    String filePath = fileTransfer.fileIndication(port);
                    if (filePath != null) {
                        System.out.println("Success: Received file '" + new File(filePath).getName() +
                                "' on port " + port + "!");
                    } else {
                        System.out.println("Error: Could not receive file on port " + port + "!");
                    }
                } catch (IOException ignored) {}
            }
        }
    }
}