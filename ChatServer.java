import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer{
    public static List<ChatUser> userList = Collections.synchronizedList(new ArrayList<ChatUser>());

    private static class SocketThread extends Thread {
        int index;

        public SocketThread(int i){
            this.index = i;
        } 

        public void run(){
            String msg;
            while(true){
                try {
                    msg = userList.get(this.index).userInput.readLine();
                    for (ChatUser user : userList) {
                        user.outputToUser.writeBytes(msg);
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

        while(true){
            Socket newConnectionOne = welcome.accept();
            SocketThread newUser = new SocketThread(userList.size()-1);
            System.out.println("new connection made");
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