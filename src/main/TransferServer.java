package main;

public class TransferServer {

    private static Integer port;

    public static void main(String[] args) {
        if (args.length > 3 || args.length < 1) {
            System.out.println("required arguments: port [loss_rate avg_delay]");
        } else {
            System.out.println("*** UDP File Transfer - Server ***");

            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid port number!");
                e.printStackTrace();
                return;
            }

            // wait for file transfer
            FileTransfer fileTransfer = new FileTransfer();
            if (fileTransfer.receiveRequest(port)) {
                System.out.println("Success: Received File");
            }
        }
    }
}