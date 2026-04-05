import java.io.*;
import java.net.*;

public class ChatClient extends Thread{
    public static DataInputStream serverOutput;
    public static void main(String argv[]) throws Exception {
        //parse arguments to variables
        String server = argv[0];
        int port = Integer.parseInt(argv[1]);
        
        Socket connection = new Socket(server,port);
        //create reader for user input, path to output user input to server, and path for messages from server
        DataOutputStream userOutput = new DataOutputStream(connection.getOutputStream());
        BufferedReader userInput = new BufferedReader (new InputStreamReader(System.in));
        serverOutput = new DataInputStream(connection.getInputStream());

        ChatClient readingThread = new ChatClient();
        readingThread.start();

        String userMessage = userInput.readLine();
        userOutput.writeBytes(userMessage + '\n');
        System.out.println(userMessage);
        
        readingThread.join();

        connection.close();

    }

    public void run(){
        
        String msg1, msg2;
        try {
            msg1 = serverOutput.readUTF();  
            System.out.println(msg1);
        
            msg2 = serverOutput.readUTF();
            System.out.println(msg2);
        } catch (IOException e) {
            System.err.println("ioexception");
        } 
    }
}
