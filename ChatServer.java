import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer extends Thread{
    public static void main(String argv[]) throws Exception {
        
        //get port from arguments
        int port = Integer.parseInt(argv[0]);

        //set up welcome socket
        ServerSocket welcome = new ServerSocket(port);

        
        //set up list of users
        List users = Collections.synchronizedList(new LinkedList<ChatUser>());
        //start connection accepting loop

            //accept new connection with welcome socket and then create a new user with it
        Socket newConnectionOne = welcome.accept();
        users.add(ChatUser(newConnectionOne, String.valueOf(users.size())), "lobby");
        ChatServer userThread = new ChatServer();
        userThread.start();
        Socket newConnectionTwo = welcome.accept();
        users.add(ChatUser(newConnectionOne, String.valueOf(users.size())), "lobby");
        
        String messageTwo = messageTwo.copyValueOf(users.get(1).userInput.readUTF());
        for(int i = 0; i<users.size(); i++){
            users.get(i).outputToUser.writeBytes(messageTwo+'\n');
        }
        
        userThread.join();

        for(int i = 0; i<users.size(); i++){
            users.get(i).connectionSocket.close;
        }
    }

    //thread for each user
    public void run(){
        String messageOne = messageOne.copyValueOf(users.get(0).userInput.readUTF());
        for(int j = 0; j<users.size(); j++){
            users.get(j).outputToUser.writeBytes(messageOne+'/n');
        }
    }
}

public class ChatUser{
    public Socket connectionSocket;
    public DataInputStream userInput;
    public DataOutputStream outputToUser;
    public String nickname;
    public String room = "lobby";

    /*@Override
    boolean equals(ChatUser user){
        if()
    }*/

    public ChatUser(Socket connection, String name, String room){
        this.connectionSocket = connection;
        this.userInput = new DataInputStream(connection.getInputStream());
        this.outputToUser = new DataOutputStream(connection.getOutputStream());
        this.nickname = name;
        this.room = room;
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