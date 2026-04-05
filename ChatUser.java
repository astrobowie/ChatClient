import java.io.*;
import java.net.*;

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

    public ChatUser(Socket connection, String name, String room) throws IOException{
        this.connectionSocket = connection;
        this.userInput = new DataInputStream(this.connectionSocket.getInputStream());
        this.outputToUser = new DataOutputStream(this.connectionSocket.getOutputStream());
        this.nickname = name;
        this.room = room;
    }

}