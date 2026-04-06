import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer{
    //list of users is a public static variable so that it can be accessed inside threads
    public static List<ChatUser> userList = Collections.synchronizedList(new ArrayList<ChatUser>());

    //new type of thread that takes an int so that it knows which user to listen for
    private static class SocketThread extends Thread {
        int index;

        public SocketThread(int i){
            this.index = i;
        } 

        public void run(){
            String msg;
            while(true){
                //listen for messages from user, send them to all other users, watch out for io errors, do this forever
                try {
                    msg = userList.get(this.index).userInput.readLine();
                    for (int i = 0; i<userList.size(); i++) {
                        userList.get(i).outputToUser.writeBytes(msg+'\n');
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String argv[]) throws Exception {
        
        //get port from arguments
        int port = Integer.parseInt(argv[0]);

        //set up welcome socket
        ServerSocket welcome = new ServerSocket(port);

        //welcome new connections forever
        while(true){
            Socket newConnectionOne = welcome.accept();
            //make new thread to handle new user connection messages
            SocketThread newUser = new SocketThread(userList.size());
            System.out.println("new connection made");

            //add new user to list and start new thread
            userList.add(new ChatUser(newConnectionOne, String.valueOf(userList.size()), "lobby"));
            newUser.start();
        }
        
    }

    
}




/*
to do:
- figure out what to do for the equivalence of user class
- set up threads
- set up list accumulation
- set up 
*/