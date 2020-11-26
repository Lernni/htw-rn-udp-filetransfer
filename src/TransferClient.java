import java.io.*;
import java.net.*;

public class TransferClient {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("required arguments: hostname/ip port file");
        } else {
            System.out.println("*** UDP File Transfer - Client ***");
        }
    }
}