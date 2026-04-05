import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer extends Thread{
    public static List<ChatUser> userList;

    public static void main(String argv[]) throws Exception {
        
        //get port from arguments
        int port = Integer.parseInt(argv[0]);

        //set up welcome socket
        ServerSocket welcome = new ServerSocket(port);

        //start connection accepting loop

            //accept new connection with welcome socket and then create a new user with it
        Socket newConnectionOne = welcome.accept();
        userList.add(new ChatUser(newConnectionOne, String.valueOf(userList.size()), "lobby"));
        ChatServer userThread = new ChatServer();
        userThread.start();
        Socket newConnectionTwo = welcome.accept();
        userList.add(new ChatUser(newConnectionTwo, String.valueOf(userList.size()), "lobby"));
        
        String messageTwo = userList.get(1).userInput.readUTF();
        for(int i = 0; i<userList.size(); i++){
            userList.get(i).outputToUser.writeBytes(messageTwo+'\n');
        }
        
        userThread.join();

        for(int i = 0; i<userList.size(); i++){
            userList.get(i).connectionSocket.close();
        }
        welcome.close();
    }

    //thread for each user
    public void run(){
        String messageOne;
        try{
            messageOne = userList.get(0).userInput.readUTF();
            for(int j = 0; j<userList.size(); j++){
                userList.get(j).outputToUser.writeBytes(messageOne+'\n');
            }
        } catch (IOException e){
            System.err.println("io fail");
        }
        
        
    }
}



/* public class SocketThread implements Runnable {
    private int socketNum;

    public SocketThread(int index){
        this.socketNum = index;
    }

    public void run() {

    }
} */
/*
to do:
- figure out what to do for the equivalence of user class
- set up threads
- set up list accumulation
- set up 
*/