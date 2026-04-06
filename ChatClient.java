import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ChatClient extends Thread{
    public static DataInputStream serverOutput;
    public static void main(String argv[]) throws Exception {
        //parse arguments to variables
        String server = argv[0];
        int port = Integer.parseInt(argv[1]);
        
        //create socket to communicate with server
        Socket connection = new Socket(server,port);
        //create reader for user input, path to output user input to server, and path for messages from server
        DataOutputStream userOutput = new DataOutputStream(connection.getOutputStream());
        BufferedReader userInput = new BufferedReader (new InputStreamReader(System.in));
        serverOutput = new DataInputStream(connection.getInputStream());

        //create and start new thread to read input
        ChatClient readingThread = new ChatClient();
        readingThread.start();

        //declare message variable to store message to be sent, size variable to store size of packagte
        String userMessage;
        //accept new messages forever
        while(true){
            //get user message
            System.out.println("send message");
            userMessage = userInput.readLine();
            userMessage = userMessage + '\n';
            //send user message as length in bytes and then a series of bytes
            userOutput.writeInt(userMessage.getBytes().length);
            userOutput.write(userMessage.getBytes(), 0, userMessage.getBytes().length);
        }
    }

    public void run(){
        String msg1;
        while(true){
            try {
                //get size of message and set up appropriate byte array
                int n = serverOutput.readInt();
                byte[] messageBytes = new byte[n];
                //read message to previously established array, interpret as string
                serverOutput.read(messageBytes, 0, n);
                msg1 = new String(messageBytes, StandardCharsets.UTF_8);
                //output to user
                System.out.println(msg1);
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }
    }
}
