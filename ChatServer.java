import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ChatServer{
    //list of users is a public static variable so that it can be accessed inside threads
    public static List<ChatUser> userList = Collections.synchronizedList(new ArrayList<ChatUser>());
    //actual active users int, since the way this is set up requires some nonsense
    public static int userNum = 0;
    //date formatting variable, useful for printouts
    public static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //list of rooms, stored in public static variable so that it can be accessed in threads
    public static List<ChatRoom> rooms = Collections.synchronizedList(new ArrayList<ChatRoom>());

    //method to parse number of commands so i don't have to
    //actually while i was writing this it occurred to me that i probably could have used split to make parsing a lot of this easier
    //and more readable for that matter
    //but im not going to lie to you i'm really proud that all this string manipulation worked as well as it did first try
    //also im still a little gunshy about running out of heap space since i ran into that error a while back. so. yknow.
    public static int numArgs(String command){
        return command.split(" ").length;
    } 
    //new type of thread that handles timer management
    private static class HeartbeatThread extends Thread{
        public void run(){
            //declare system message variable to be used if a timer fails
            String sysmsg;
            while(true){
                //go through each user in the userList
                for(int i = 0; i<userList.size(); i++){
                    
                    //if the user is valid, run the timer update
                    if(userList.get(i).nickname().equals("")!=true){
                        userList.get(i).timerUpdate();
                        //check if user's pingvalue is greater than 30000
                        if(userList.get(i).pingValue()>30000){
                            System.out.println(userList.get(i).nickname() + " disconnect " + userList.get(i).pingValue());
                            //create the system message
                            sysmsg = "type:system,message:" + userList.get(i).nickname() + ": disconnected.,timestamp:" + LocalDateTime.now().format(timeFormat);
                            try{
                                //for every single user that isn't the timed out user, if the user is valid, output a system message for the disconnect
                                for(int j = 0; j<userList.size(); j++){
                                    if(i!=j&&!userList.get(j).nickname().equals("")){
                                        userList.get(j).outputToUser.writeInt(sysmsg.getBytes().length);
                                        userList.get(j).outputToUser.write(sysmsg.getBytes());
                                    }
                                }
                                //then close the timed out user's connection
                                userList.get(i).connectionSocket.close();
                            } catch (IOException e){
                                e.printStackTrace();
                            }
                            //then mark the user as nonvalid
                            userList.get(i).name("");
                            //then lower actual usercount
                            userNum--;
                        }// end if statement
                    }
                    
                    
                }//end for loop
                //since the heartbeat thread will only ever be capable of blocking during the process of timing out a user, im also using it to check
                //                                                       whether or not the userList needs to be cleaned
                //if the userList has only invalid users, clear the whole thing
                if(userNum<=0&&userList.size()!=0){
                    userList.clear();
                }
            }//end while loop
        }//end run method
    }//end thread class

    //new type of thread that takes an int so that it knows which user to listen for
    private static class SocketThread extends Thread {
        int index;
        
        public SocketThread(int i){
            this.index = i;
        } //end constructor

        //method for sending a message selectively to every person in a room
        public void messageSend(String room, int index, byte[] messageBytes){
            //get the index of every user in the room
            int[] roomUsers = rooms.get(rooms.indexOf(new ChatRoom(room))).users();
            try{
                //output messages to each user in the list of users in the room
                for (int i : roomUsers) {
                    //check to make sure that you dont send the message back to the user
                    if(i!=index&&userList.get(i).nickname().equals("")!=true){
                        //send size for message framing, then send actual bytes
                        userList.get(i).outputToUser.writeInt(messageBytes.length);
                        userList.get(i).outputToUser.write(messageBytes);
                    }
                }
            } catch (EOFException f) {
                f.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } // end try catch block
            rooms.get(rooms.indexOf(new ChatRoom(room))).historyAdd(new String(messageBytes));
        }

        public void run(){
            //declare string to use for parsing later
            String msg = "";
            String type;
            String date;
            String payload = "";
            String room;
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
                } catch (SocketException s){
                    //exit loop if socket gets closed
                    break;
                } catch (IOException e){
                    e.printStackTrace();
                    break;
                } // end try catch block
                //since we have now recieved a message from the user, we will reset the user's ping timer
                userList.get(this.index).timerReset();
                //get the type and the date since those are the ones that are always importantand always in the same spot
                date = msg.substring(msg.lastIndexOf(",timestamp:")+11);
                //the message always starts with "type:" so we just skip to index 5
                
                type = msg.substring(5,msg.indexOf(','));
                //if i wasnt specifically asked to use "a series of text fields" for the meta data i probably would not have 
                //                               put the type indicator there at all tbh
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
                                    if(numArgs(payload)!=2){
                                        type="error";
                                        payload = "Error: Invalid arguments!";
                                        break;
                                    } else {
                                        //break the room argument off on its own
                                        String roomTarget = payload.substring(6,payload.indexOf(' ',6));
                                        //check if the room argument is valid
                                        if(roomTarget.length()>0&&roomTarget.length()<=20){
                                            //if it is, check if the room already exists
                                            if(rooms.contains(new ChatRoom(roomTarget))){
                                                //if it does, move user to that room
                                                //remove user from previous room
                                                rooms.get(rooms.indexOf(new ChatRoom(userList.get(this.index).room()))).remove(this.index);
                                                //add user to new room
                                                rooms.get(rooms.indexOf(new ChatRoom(roomTarget))).add(this.index);
                                            } else {
                                                //if it doesn't, make the room and move the user to it
                                                rooms.add(new ChatRoom(roomTarget));
                                                rooms.get(rooms.indexOf(new ChatRoom(userList.get(this.index).room()))).remove(this.index);
                                                //add user to new room
                                                rooms.get(rooms.indexOf(new ChatRoom(roomTarget))).add(this.index);
                                            }
                                            //then send a system message to the user
                                            String userMoveMsg = "type:system,message:room" + roomTarget + ",timestamp:" + LocalDateTime.now().format(timeFormat);
                                            try {
                                                userList.get(this.index).outputToUser.writeInt(userMoveMsg.getBytes().length);
                                                userList.get(this.index).outputToUser.write(userMoveMsg.getBytes());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            //then send a system message to the people in the user's old room
                                            String otherMoveMsg = "type:system,message:" + userList.get(this.index).nickname() + ": joined room " + roomTarget + ".,timestamp:" + LocalDateTime.now().format(timeFormat);
                                            messageSend(userList.get(this.index).room(),this.index,otherMoveMsg.getBytes());
                                            //then send a system message to the people in the user's new room
                                            messageSend(roomTarget,this.index,otherMoveMsg.getBytes());
                                            //then update the room on the server side user list
                                            userList.get(this.index).newRoom(roomTarget);
                                            //if i were building this from the ground up to my own specifications i would probably make this look cleaner
                                            //as it stands the need to match both the specifications and make it work on what i've already build is kinda killing this
                                            //thats fine though i guess
                                            //anyway now you do the history
                                            //get the history from the room
                                            String historyString = rooms.get(rooms.indexOf(new ChatRoom(roomTarget))).historyGet() + ",timestamp:" + LocalDateTime.now().format(timeFormat);
                                            try {
                                                //frame and send the history
                                                userList.get(this.index).outputToUser.writeInt(historyString.getBytes().length);
                                                userList.get(this.index).outputToUser.write(historyString.getBytes());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            type="error";
                                            payload = "Error: Invalid arguments!";
                                            break;
                                        }
                                    }
                                    System.out.println("join");
                                    break;
                                case "msg":
                                    //check if arguments are legal
                                    if(numArgs(payload)<3){ //<-- this line has a heart in it! yay
                                        //if arguments are not legal, get mad at the user
                                        type="error";
                                        payload = "Error: Invalid arguments!";
                                        break;
                                    }
                                    //get person user is trying to dm
                                    String dmTarget = payload.substring(5,payload.indexOf(' ',5));
                                    //check if user is real
                                    if(userList.contains(new ChatUser(dmTarget))){
                                        //replace message with pm metadata and payload
                                        msg = "type:pm,from:" + dmTarget + ",text:[PM from " + dmTarget +"] " + payload.substring(payload.indexOf(dmTarget)+dmTarget.length()) + ",timestamp:" + date;
                                        //get new message size
                                        int pmSize = msg.getBytes().length;
                                        try{
                                            //send dm to target w/ message framing
                                            userList.get(userList.indexOf(new ChatUser(dmTarget))).outputToUser.writeInt(pmSize);
                                            userList.get(userList.indexOf(new ChatUser(dmTarget))).outputToUser.write(msg.getBytes(), 0, pmSize);
                                            
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
                                    //remove user from previous room
                                    rooms.get(rooms.indexOf(new ChatRoom(userList.get(this.index).room()))).remove(this.index);
                                    //add user to new room
                                    rooms.get(rooms.indexOf(new ChatRoom("lobby"))).add(this.index);
                                    //then send a system message to the user
                                    String userMoveMsg = "type:system,message:roomlobby,timestamp:" + LocalDateTime.now().format(timeFormat);
                                    try {
                                        userList.get(this.index).outputToUser.writeInt(userMoveMsg.getBytes().length);
                                        userList.get(this.index).outputToUser.write(userMoveMsg.getBytes());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    //then send a system message to the people in the user's old room
                                    String otherMoveMsg = "type:system,message:" + userList.get(this.index).nickname() + ": joined room lobby.,timestamp:" + LocalDateTime.now().format(timeFormat);
                                    messageSend(userList.get(this.index).room(),this.index,otherMoveMsg.getBytes());
                                    //then send a system message to the people in the user's new room
                                    messageSend("lobby",this.index,otherMoveMsg.getBytes());
                                    //then update the room on the server side user list
                                    userList.get(this.index).newRoom("lobby");
                                    System.out.println("leave");
                                    break;
                                case "rooms":
                                    //declare list of all rooms, start with meta data
                                    String roomList = "type:system,message:";
                                    //add the name of every room to the message
                                    for(int i = 0; i<rooms.size(); i++){
                                        roomList += rooms.get(i).name() + ", ";
                                    }
                                    //remove the last comma from the list
                                    roomList = roomList.substring(0,roomList.length()-2);
                                    //add final meta data
                                    roomList += ",timestamp:" + LocalDateTime.now().format(timeFormat);
                                    //frame and send message
                                    try {
                                        userList.get(this.index).outputToUser.writeInt(roomList.getBytes().length);
                                        userList.get(this.index).outputToUser.write(roomList.getBytes());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("rooms");
                                    break;
                                case "who":
                                    //make string with metadata
                                    String roomUsers = "type:system,message:";
                                    //get user indexes from current room
                                    int[] roomUserIndex = rooms.get(rooms.indexOf(new ChatRoom(userList.get(this.index).room()))).users();
                                    //add names of all users
                                    for(int i : roomUserIndex){
                                        roomUsers += userList.get(i).nickname() + ", ";
                                    }
                                    //remove the last comma from the list
                                    roomUsers = roomUsers.substring(0,roomUsers.length()-2);
                                    //add final meta data
                                    roomUsers += ",timestamp:" + LocalDateTime.now().format(timeFormat);
                                    //frame and send message
                                    try {
                                        userList.get(this.index).outputToUser.writeInt(roomUsers.getBytes().length);
                                        userList.get(this.index).outputToUser.write(roomUsers.getBytes());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
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
                                        userList.get(this.index).name(payload.substring(payload.indexOf(' ')+1,payload.length()-1));
                                        msg = "type:system,message:nick" + userList.get(this.index).nickname() + ",timestamp:" + LocalDateTime.now().format(timeFormat);
                                        try {
                                            userList.get(this.index).outputToUser.writeInt(msg.getBytes().length);
                                            userList.get(this.index).outputToUser.write(msg.getBytes());
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
                            //get user's room from message
                            room = msg.substring(msg.indexOf(",room:")+6,msg.lastIndexOf(",nickname:"));
                            //send message to every user in the room who isnt you
                            messageSend(room, this.index, messageBytes);
                        }
                        break;
                    case "ping":
                        System.out.println("Ping " + userList.get(this.index).nickname()+ " " + userList.get(this.index).pingValue());
                        break;
                    case "disconnect":
                        //on disconnect message, send system message to all users in the room
                        //construct exit message
                        String exitMessage = "type:system,message:" + userList.get(this.index).nickname() + " disconnected,timestamp:"+ LocalDateTime.now().format(timeFormat);
                        //send the message
                        messageSend(userList.get(this.index).room(), n, exitMessage.getBytes());
                        //also remove the user from their room
                        rooms.get(rooms.indexOf(new ChatRoom(userList.get(this.index).room()))).remove(this.index);
                        //also close the socket and mark userlist slot for reuse
                        System.out.println("disconnect");
                        try{
                            userList.get(this.index).connectionSocket.close();
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                        userList.get(index).name("");
                        //also decrement the actual user number
                        userNum--;
                        
                        break;
                    case "register":
                        //if the type is a register ping, check to see if the new nickname is already in the list
                        if(userList.contains(new ChatUser(msg.substring(msg.indexOf(",nickname:")+10, msg.lastIndexOf(",userID:"))))){
                            //if it is, return an error message
                            userNum--;
                            type = "error";
                            payload = "Error: Username already registered";
                            userList.get(this.index).nickname = "";
                        } else {
                            //otherwise, set the new nickname
                            userList.get(this.index).name(msg.substring(msg.indexOf(",nickname:")+10, msg.lastIndexOf(",userID:")));
                            System.out.println("Registered");
                            //then add user to lobby
                            rooms.get(0).add(this.index);
                            //then send ok message
                            msg = "type:ok,message:registered,room:lobby,timestamp:" + date;
                            //message framing, yada yada, this does the same thing all the other try-catch blocks do, you get it by now
                            try {
                                userList.get(this.index).outputToUser.writeInt(msg.getBytes().length);
                                userList.get(this.index).outputToUser.write(msg.getBytes());
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
                    msg = "type:error,message:" + payload + ",timestamp:" + LocalDateTime.now().format(timeFormat);
                    try {
                        userList.get(this.index).outputToUser.writeInt(msg.getBytes().length);
                        userList.get(this.index).outputToUser.write(msg.getBytes());
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    //set the thread to end if the error nulled the user
                    if(userList.get(this.index).nickname().equals("")){
                        type = "disconnect";
                    }
                }
                //end while loop if type is disconnect, and in so doing end the thread
                if(type.equals("disconnect")){ 
                    System.out.println(rooms.get(rooms.indexOf(new ChatRoom("lobby"))).historyGet());
                    break;
                }
            }//end while loop
        }//end run method
    }//end private subclass

    @SuppressWarnings({ "resource" })
    public static void main(String argv[]) throws Exception {
        
        //get port from arguments
        int port = Integer.parseInt(argv[0]);

        //set up welcome socket
        ServerSocket welcome = new ServerSocket(port);

        //set up thread to maintain pings and kick dc'd users
        HeartbeatThread pings = new HeartbeatThread();
        pings.start();

        //create lobby room
        rooms.add(new ChatRoom("lobby"));

        //welcome new connections forever
        while(true){
            Socket newConnectionOne = welcome.accept();
            SocketThread newUser;
            //check if existing ChatUser object can be repurposed
            if(userList.contains(new ChatUser(""))){
                //if so, make a new thread and repurpose the old userList spot
                userNum++;
                newUser = new SocketThread(userList.indexOf(new ChatUser("")));
                userList.get(userList.indexOf(new ChatUser(""))).reUseUser(newConnectionOne, String.valueOf(userList.size()), "lobby");
                
            } else {
                //if not, add a new user and then start a brand new thread
                userNum++;
                userList.add(new ChatUser(newConnectionOne, String.valueOf(userList.size()), "lobby"));
                newUser = new SocketThread(userList.size()-1);
            }
            
            System.out.println("new connection made");

            //add new user to list and start new thread
            newUser.start();
        } // end while loop
    } //end main
}// end class