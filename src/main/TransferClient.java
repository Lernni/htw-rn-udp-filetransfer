package main;

import sw.RateMeasurement;

import java.io.*;
import java.net.*;

public class TransferClient {
    public static void main(String[] args) throws Exception {
        if (args.length > 4 || args.length < 3)  {
            System.out.println("required arguments: <hostname/ip> <port> <file> ['debug']");
        } else {
            System.out.println("*** UDP File Transfer - Client ***\n");

            // initialize variables
            InetAddress host;
            try {
                host = InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                System.out.println("Error: Please enter a valid host!");
                return;
            }

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

            boolean debugMode = false;
            if (args.length == 4) debugMode = (args[3].equals("debug"));

            // print client info
            InetAddress client = InetAddress.getLocalHost();
            System.out.println("Client Info:\n" +
                    "Host Name: " + client.getHostName() + "\n" +
                    "IP Address: " + client.getHostAddress() + "\n");

            System.out.println("Server Info:\n" +
                    "Host Name: " + host.getHostName() + "\n" +
                    "IP Address: " + host.getHostAddress() + "\n" +
                    "Port: " + port + "\n");

            System.out.println("File Info:\n" +
                    "File Name: " + file.getName() + "\n" +
                    "File Size: " + RateMeasurement.getReadableByte(file.length(), "B") + "\n");

            // start file transfer
            FileTransfer fileTransfer = new FileTransfer(debugMode);
            if (fileTransfer.fileRequest(host, port, file)) {
                System.out.println("Success: File '" + file.getName() + "' was sent successfully to host '" +
                        host.getHostName() + "' on port " + port + "!");
            } else {
                System.out.println("Error: File '" + file.getName() + "' could not be sent to host '" +
                        host.getHostName() + "' on port " + port + "!");
            }

            System.exit(0);
        }
    }
}