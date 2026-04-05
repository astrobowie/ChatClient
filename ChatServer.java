import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer extends Thread{
    public static List<ChatUser> userList = Collections.synchronizedList(new LinkedList<ChatUser>());

    public static void main(String argv[]) throws Exception {
        
        //get port from arguments
        int port = Integer.parseInt(argv[0]);

        //set up welcome socket
        ServerSocket welcome = new ServerSocket(port);

        //open first connection, use it to make first user
        System.out.println("waiting for connection");
        Socket newConnectionOne = welcome.accept();
        System.out.println("first connection made");
        userList.add(new ChatUser(newConnectionOne, String.valueOf(userList.size()), "lobby"));
        
        //accept second connection, add it to user list
        Socket newConnectionTwo = welcome.accept();
        userList.add(new ChatUser(newConnectionTwo, String.valueOf(userList.size()), "lobby"));
        System.out.println("second connection made");
        
        //start new thread for first connection
        ChatServer userThread = new ChatServer();
        userThread.start();

        //get the message
        String messageTwo = userList.get(1).userInput.readLine();
        for(int i = 0; i<userList.size(); i++){
            userList.get(i).outputToUser.writeBytes(messageTwo+'\n');
        }
        
        //wait for other list
        userThread.join();

        //close connections and return
        for(int i = 0; i<userList.size(); i++){
            userList.get(i).connectionSocket.close();
        }
        welcome.close();
        return;
    }

    //thread for each user
    public void run(){
        String messageOne;
        try{
            messageOne = userList.get(0).userInput.readLine();
            for(int j = 0; j<userList.size(); j++){
                userList.get(j).outputToUser.writeBytes(messageOne+'\n');
            }
        } catch (IOException e){
            e.printStackTrace();
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