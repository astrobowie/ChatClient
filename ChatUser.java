import java.io.*;
import java.net.*;

public class ChatUser{
    //socket used to connect to user
    public Socket connectionSocket;
    //input and output streams from the socket
    public DataInputStream userInput;
    public DataOutputStream outputToUser;
    //nickname and userID
    public String nickname;
    public String userID = "temp";
    //room
    public String room = "lobby";
    //variables counting time since
    public long lastPing = System.currentTimeMillis();
    public int pingTimer = 0;

    //equals override
    @Override
    public boolean equals(Object user){
        //i wanted this to check based on a chatuser or a string so i can use it for direct messages
        //                       and checking duplicate nicknames with ArrayList's contains() method
        //turns out it doesnt work like that for like anything
        //keeping the string part of that around just in case it becomes useful later
        //anyway we start by checking if the object compared is a chatuser
        if(user instanceof ChatUser){
            //create variable compared equal to user if it was a chat user
            ChatUser compared = (ChatUser) user;
            //check if the user's nickname is equal, if it is, return true, otherwise return false
            if(compared.nickname.equals(this.nickname)){
                return true;
            } else {
                return false;
            }
        } else { // end chat user check
            //start string check
            if (user instanceof String) {
                //second verse, same as the first
                //make a variable to return the user argument as a string
                String compared = (String) user;
                //if it equals the nickname, it's the same, otherwise, it isnt
                if(compared.equals(this.nickname)){
                    return true;
                } else {
                    return false;
                }
            } else { //end string check
                //return false if it isn't a chat user or a string
                return false;
            } //end if
        }// end else
    }// end equals

    //so there are like, timer classes and stuff in java.util, but they all use a lot of threading
    //and im already using a *lot* of threading, to the point im a bit worried about memory usage
    //so instead of using that, i'm gonna pull a trick from gamedev and just make a delta variable
    //turns out because all the other threads are running halting expressions that still requires
    //                               that i make a thread, but its still probably better to have
    //                               one thread than 20
    //tl;dr: this is used to keep track of how long it's been since a ping
    public int timerUpdate(){
        //set a variable to the current time so it stays consistent
        long currTime = System.currentTimeMillis();
        //increase the ping timer to however long it's been since the last ping
        pingTimer = pingTimer + (int) (currTime - this.lastPing); 
        //update the timer since the last ping
        lastPing = currTime;
        //return total time since last ping
        return pingTimer;
    }
    //usually this trick is used to keep track of how much time has passed between frames so that you can
    //                 make sure your processes aren't determined on how quickly the frames are rendering
    //not actually relevant i just thought it was cool

    //while i'm adding useless trivia in the comments i may as well add that i coded the first iteration of
    //                                this on a whim while i was supposed to be programming the first draft 
    //                                of the expanded message parsing system to handle metadata and stuff
    //which meant that since i wanted to set up my git commits in a manner approaching reasonable i couldnt
    //                                actually test it until i had a more basic version of the switch case
    //                                trees done
    //what they dont tell you about adhd is that it gives a flat debuff to your ability to implement best
    //                                practice while coding

    //constructor method
    //i should probably say something about this but for the life of me i can't figure out what
    //it takes strings and a connection and then it makes the variables equal to those
    //youre a smart cookie you can figure it out
    public ChatUser(Socket connection, String name, String room) throws IOException{
        this.connectionSocket = connection;
        this.userInput = new DataInputStream(this.connectionSocket.getInputStream());
        this.outputToUser = new DataOutputStream(this.connectionSocket.getOutputStream());
        this.nickname = name;
        this.room = room;
    }
    //overloaded constructor to very quickly get around the fact that contains doesn't work like i thought it did
    public ChatUser(String name){
        this.connectionSocket = null;
        this.userInput = null;
        this.outputToUser = null;
        this.nickname = name;
        this.room = null;
    }

    public void reUseUser(Socket connection, String name, String room) throws IOException{
        this.connectionSocket = connection;
        this.userInput = new DataInputStream(this.connectionSocket.getInputStream());
        this.outputToUser = new DataOutputStream(this.connectionSocket.getOutputStream());
        this.nickname = name;
        this.room = room;
        this.lastPing=System.currentTimeMillis();
        this.pingTimer=0;
    }
}