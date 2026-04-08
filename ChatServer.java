import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer{
    //list of users is a public static variable so that it can be accessed inside threads
    public static List<ChatUser> userList = Collections.synchronizedList(new ArrayList<ChatUser>());

    //method to parse number of commands so i don't have to
    //actually while i was writing this it occurred to me that i probably could have used split to make parsing a lot of this easier
    //and more readable for that matter
    //but im not going to lie to you i'm really proud that all this string manipulation worked as well as it did first try
    //also im still a little gunshy about running out of heap space since i ran into that error a while back. so. yknow.
    public static int numArgs(String command){
        return command.split(" ").length;
    } 
    //new type of thread that takes an int so that it knows which user to listen for
    private static class SocketThread extends Thread {
        int index;

        public SocketThread(int i){
            this.index = i;
        } //end constructor

        @SuppressWarnings("unlikely-arg-type")
        public void run(){
            //declare string to use for parsing later
            String msg = "";
            String type;
            String date;
            String payload = "";
            int n = 0;
            byte[] messageBytes = null;

            //listen for messages from the given user
            while(true){
                try {
                    //read in size of message
                    n = userList.get(this.index).userInput.readInt();
                    //create array to store message
                    messageBytes = new byte[n];
                    //write rest of message to array, convert array to string
                    userList.get(this.index).userInput.read(messageBytes, 0, n);
                    msg = new String(messageBytes);
                } catch (EOFException f) {
                    //exit loop if client closes
                    break;
                } catch (IOException e){
                    e.printStackTrace();
                    break;
                } // end try catch block
                //get the type and the date since those are the ones that are always importantand always in the same spot
                date = msg.substring(msg.lastIndexOf(",timestamp:")+11);
                //the message always starts with "type:" so we just skip to index 5
                
                type = msg.substring(5,msg.indexOf(','));
                //if i wasnt specifically asked to use "a series of text fields" for the meta data i probably would not have 
                //                               put that indicator there tbh
                //but im already playing kinda fast and loose by just making the whole thing one string that i parse for 10 years
                //so whatever
                //in my defense figuring out how to reconstruct a json file from bytes when its probably just going to involve
                //                               sending the json as a string and then constructing a new json when it arrives 
                //                               sounds really annoying and we were pretty explicitly told to handle this however


                //depending on the type of message, do different stuff
                switch (type) {
                    //if the message is text, get the payload
                    case "text":
                        //uses "lastindexof" for timestamp so that the phrase ",timestamp:" in a message won't beef the whole program
                        //also adds an extra line of white space so that the upcoming switch case doesn't brick itself on single argument commands
                        //that's not an ellegant solution but the alternative was throwing a ternary operator into the switch case statement
                        //which i think would put me on a list somewhere
                        payload = msg.substring(msg.indexOf(",text:")+6, msg.lastIndexOf(",timestamp:")) + " ";
                        //check if the message is a command
                        if(payload.charAt(0)=='/'){
                            //if it is a command, do different stuff depending on the command
                            switch (payload.substring(1, payload.indexOf(' '))){
                                case "join":
                                    if(numArgs(payload)<2){
                                        type="error";
                                        payload = "Error: Invalid arguments!";
                                        break;
                                    }
                                    System.out.println("join");
                                    break;
                                case "msg":
                                    //check if arguments are legal
                                    if(numArgs(payload)<3){ //<-- this line has a heart in it! yay
                                        type="error";
                                        payload = "Error: Invalid arguments!";
                                        break;
                                    }
                                    //get person user is trying to dm
                                    String dmTarget = payload.substring(5,payload.indexOf(' ',5));
                                    //check if user is real
                                    if(userList.contains(new ChatUser(dmTarget))){
                                        //replace message with pm metadata and payload
                                        msg = "type:pm,from:" + dmTarget + ",text:[PM from " + dmTarget +"] " + payload.substring(payload.indexOf(dmTarget)+dmTarget.length()) + "timestamp:" + date;
                                        //get new message size
                                        int pmSize = msg.getBytes().length;
                                        try{                                        
                                            //send dm to target w/ message framing
                                            userList.get(userList.indexOf(dmTarget)).outputToUser.writeInt(pmSize);
                                            userList.get(userList.indexOf(dmTarget)).outputToUser.write(messageBytes, 0, pmSize);
                                            
                                        } catch (EOFException f) {
                                            f.printStackTrace();
                                        } catch (IOException e){
                                            e.printStackTrace();
                                        } // end try catch block
                                    } else { //if user isnt real send back error ping
                                        type = "error";
                                        payload = "Error: User " + dmTarget + " does not exist!";
                                    }
                                    break;
                                case "leave":
                                    System.out.println("leave");
                                    break;
                                case "rooms":
                                    System.out.println("rooms");
                                    break;
                                case "who":
                                    System.out.println("who");
                                    break;
                                case "nick":
                                    if(numArgs(payload)<2){
                                        type="error";
                                        payload = "Error: Invalid arguments!";
                                        break;
                                    }
                                    //in the case of the nick command, check if the given nickname is already in the list
                                    if(userList.contains(new ChatUser(payload.substring(payload.indexOf(' '))))){
                                        //return an error message if it is
                                        type = "error";
                                        payload = "Error: Username already registered";
                                    } else {
                                        //if it isnt, update the nickname
                                        userList.get(this.index).nickname = payload.substring(payload.indexOf(' ')+1,payload.length()-1);
                                        msg = "type:system,message:nick" + userList.get(this.index).nickname + ",timestamp:" + date;
                                        try {
                                            userList.get(this.index).outputToUser.writeInt(msg.getBytes().length);
                                            userList.get(this.index).outputToUser.write(msg.getBytes(), 0, msg.getBytes().length);
                                        } catch (IOException e){
                                            e.printStackTrace();
                                            break;
                                        }
                                    }
                                    break;
                                default:
                                    payload = "Error: Command not recognized.";
                                    type = "error";
                                    break;
                            } //end command switch case statement
                        } //end command handling if-statement
                        //else statement for if its just, like, a normal text message
                        else { 
                            try{
                                //output messages to each user in the list
                                for (int i = 0; i<userList.size(); i++) {
                                    //check to make sure that you dont send the message back to the user
                                    if(i!=this.index){
                                        //send size for message framing, then send actual bytes
                                        userList.get(i).outputToUser.writeInt(n);
                                        userList.get(i).outputToUser.write(messageBytes, 0, n);
                                    }
                                }
                            } catch (EOFException f) {
                                f.printStackTrace();
                            } catch (IOException e){
                                e.printStackTrace();
                            } // end try catch block
                        }
                        break;
                    case "ping":    
                        System.out.println("Ping");
                        break;
                    case "disconnect":
                        //on disconnect message, close the socket and mark userlist slot for reuse
                        System.out.println("disconnect");
                        try{
                            userList.get(index).connectionSocket.close();
                            userList.get(index).nickname = "nullUser";
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                        break;
                    case "register":
                        //if the type is a register ping, check to see if the new nickname is already in the list
                        if(userList.contains(msg.substring(msg.indexOf(",nickname:")+10, msg.lastIndexOf(",timestamp:")))){
                            //if it is, return an error message
                            type = "error";
                            payload = "Error: Username already registered";
                        } else {
                            //otherwise, set the new nickname
                            userList.get(this.index).nickname = msg.substring(msg.indexOf(",nickname:")+10, msg.lastIndexOf(",timestamp:"));
                            System.out.println("Registered");
                            //then send ok message
                            msg = "type:ok,message:registered,room:lobby,timestamp:" + date;
                            //message framing, yada yada, this does the same thing all the other try-catch blocks do, you get it by now
                            try {
                                userList.get(this.index).outputToUser.writeInt(msg.getBytes().length);
                                userList.get(this.index).outputToUser.write(msg.getBytes(), 0, msg.getBytes().length);
                            } catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                        break;
                    default:
                        break;
                } //end message type switch-case statement

                //if any of the commands or pings returned an error, return an error message to the user
                if(type.equals("error")){
                    System.out.println(payload);
                    msg = "type:error,message:" + payload + ",timestamp:" + date;
                    try {
                        userList.get(this.index).outputToUser.writeInt(msg.getBytes().length);
                        userList.get(this.index).outputToUser.write(msg.getBytes(), 0, msg.getBytes().length);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
                //end while loop if type is disconnect
                if(type.equals("disconnect")){ 
                    break;
                }
            }//end while loop
        }//end run method
    }//end private subclass

    @SuppressWarnings({ "resource", "unlikely-arg-type" })
    public static void main(String argv[]) throws Exception {
        
        //get port from arguments
        int port = Integer.parseInt(argv[0]);

        //set up welcome socket
        ServerSocket welcome = new ServerSocket(port);

        //welcome new connections forever
        while(true){
            Socket newConnectionOne = welcome.accept();
            SocketThread newUser;
            //check if existing ChatUser object can be repurposed
            if(userList.contains("nullUser")){
                //if so, make a new thread and repurpose the old userList spot
                newUser = new SocketThread(userList.indexOf("nullUser"));
                userList.get(userList.indexOf("nullUser")).reUseUser(newConnectionOne, String.valueOf(userList.size()), "lobby");
                
            } else {
                //if not, add a new user and then start a brand new thread
                userList.add(new ChatUser(newConnectionOne, String.valueOf(userList.size()), "lobby"));
                newUser = new SocketThread(userList.size());
            }
            
            System.out.println("new connection made");

            //add new user to list and start new thread
            userList.add(new ChatUser(newConnectionOne, String.valueOf(userList.size()), "lobby"));
            newUser.start();
        } // end while loop
    } //end main
}// end class