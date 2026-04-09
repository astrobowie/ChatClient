import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ChatClient extends Thread{
    public static DataInputStream serverOutput;
    public static DataOutputStream userOutput;
    public static String nickname;
    public static boolean leave = false;
    public static String server;
    public static int port;
    public static String room;
    public static String userID;
    public static String startDate;
    public static long lastPing = System.currentTimeMillis();
    public static int pulseTimer=0;
    public static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static Object lock = new Object();
    
    public static synchronized void pingSent(){
        lastPing = System.currentTimeMillis();
        pulseTimer = 0;
    }

    //thread to handle heartbeat pings to server
    private static class pingThread extends Thread{
        public void run(){
            //declare time variable and string to hold message
            long currTime;
            String pingMsg;
            while(!leave){
                currTime = System.currentTimeMillis();
                pulseTimer += (int) currTime - lastPing;
                lastPing = currTime;
                if(pulseTimer>10000){
                    pingMsg = "type:ping,nickname:"+ nickname +",userID:" + userID + ",timestamp:" + LocalDateTime.now().format(timeFormat);
                        try {
                            userOutput.writeInt(pingMsg.getBytes().length);
                            userOutput.write(pingMsg.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    pingSent();
                }
            }
        }
    } 

    public static void main(String argv[]) throws Exception {
        //parse arguments to variables
        server = argv[0];
        port = Integer.parseInt(argv[1]);
        nickname = argv[2];
        userID = argv[3];
        
        //date and time stuff
        LocalDateTime timeKeeper;
        

        //create socket to communicate with server
        Socket connection = new Socket(server,port);
        //create reader for user input, path to output user input to server, and path for messages from server
        userOutput = new DataOutputStream(connection.getOutputStream());
        BufferedReader userInput = new BufferedReader (new InputStreamReader(System.in));
        serverOutput = new DataInputStream(connection.getInputStream());

        //create and start new thread to read input
        ChatClient readingThread = new ChatClient();
        readingThread.start();

        //declare message variable to store message to be sent, size variable to store size of package
        String userMessage;

        //send register message
        timeKeeper = LocalDateTime.now();
        userMessage = "type:register,nickname:" + nickname + ",userID:" + userID + ",timestamp:" + timeKeeper.format(timeFormat);
        synchronized (lock) {
            userOutput.writeInt(userMessage.getBytes().length);
            userOutput.write(userMessage.getBytes(), 0, userMessage.getBytes().length);
        }
        room = "lobby";

        //set up heartbeat thread
        pingThread heartbeat = new pingThread();
        heartbeat.start();

        //accept new messages forever
        while(!leave){
            //get user message
            userMessage = userInput.readLine();
            //checks if user message is a disconnect, send disconnect message instead
            //also get current datetime
            timeKeeper = LocalDateTime.now();
            if(userMessage.length()>=11&&userMessage.substring(0,11).equals("/disconnect")){
                userMessage = "type:disconnect,nickname:" + nickname + ",userID:" + userID + ",timestamp:"+ timeKeeper.format(timeFormat);
                leave = true;
            } else {
                //otherwise, add metadata to user message and send it
                userMessage = "type:text,room:"+ room +",nickname:" + nickname + ",userID:" + userID + ",text:" + userMessage + ",timestamp:" + timeKeeper.format(timeFormat);
            } // end if else
            //send message as length in bytes and then a series of bytes
            synchronized(lock){
                userOutput.writeInt(userMessage.getBytes().length);
                userOutput.write(userMessage.getBytes());
            }
            
            //tell the heartbeat timer that a message has been sent
            pingSent();
        }//end while loop
        //on disconnect, close connection
        System.out.println("Disconnect Successful");
        connection.close();
    }

    public void run(){
        //initialize string variables
        String msg1 = "error";
        String payload = "";
        String date;
        String type;
        //initializing this to null is probably bad practice but doing the fifty lines of string manipulation inside a try catch statement strikes me as worse
        //so we ball
        byte[] messageBytes = null;
        while(!leave){
            try {
                //get size of message and set up appropriate byte array
                int n = serverOutput.readInt();
                messageBytes = new byte[n];
                //read message to previously established array, interpret as string
                serverOutput.read(messageBytes, 0, n);
                msg1 = new String(messageBytes, StandardCharsets.UTF_8);
                //output to user
            } catch (IOException e) {
                //print exception and then exit loop so that the thread doesnt nuke my terminal with error messages
                e.printStackTrace();
                leave = true;
                break;
            } 
            //if the message got read properly, we gotta spend forever parsing it
            if(msg1.equals("error")!=true){
                //start by getting type and date since those are present in all packages
                date = msg1.substring(msg1.lastIndexOf(",timestamp:")+11);
                type = msg1.substring(5,msg1.indexOf(','));
                //add date to start of payload, since that needs doing no matter what
                payload = date + " :: ";
                //switch case statement to determine what to do based on the type
                switch (type) {
                    case "ok":
                        //on the thing of print starting info
                        payload = "ChatClient started with server IP: " + server + ", port: " + port + ", nickname:" + nickname + ", client ID: " + userID + ", Date/Time: " + date + "\n";
                        //also save start date here
                        startDate = date;
                        break;
                    case "error":
                        //check if error message comes from registration failure, then break 
                        if(msg1.substring(msg1.indexOf(",message:")+9,msg1.lastIndexOf(",timestamp:")).equals("Error: Username already registered")){
                            leave=true;
                        }
                        payload += msg1.substring(msg1.indexOf(",message:")+9,msg1.lastIndexOf(",timestamp:"));
                        break;
                    //this is supposed to be "deliver" but ill be honest i dont see a reason to futz with the message framing for this kind of message
                    //on a real chat client it might be a security risk i guess? idk i dont think it matters
                    //the server can just relay the message directly from other chat clients
                    case "text":
                        //append the room name and the sender to the payload, then append the actual message
                        payload += "[" + msg1.substring(msg1.indexOf(",room:")+6,msg1.lastIndexOf(",nickname:")) + "] " + msg1.substring(msg1.indexOf(",nickname:")+10,msg1.lastIndexOf(",userID")) + ": " + msg1.substring(msg1.indexOf(",text:")+6, msg1.lastIndexOf(",timestamp:"));
                        break;
                    case "pm":
                        //i probably could have processed the pm message indicator here instead of on the server side in retrospect
                        //whatever
                        //just get the text from the message and append it to the payload
                        payload += msg1.substring(msg1.indexOf(",text:")+6, msg1.lastIndexOf(",timestamp:"));
                        break;
                    case "system":
                        //if system confirms nickname change with message starting in nick, change local nickname variable to rest of message
                        if(msg1.substring(msg1.indexOf(",message:")+9,msg1.indexOf(",message:")+9+4).equals("nick")){
                            nickname = msg1.substring(msg1.indexOf(",message:")+9+4,msg1.lastIndexOf(",timestamp:"));
                        } else {
                        if(msg1.substring(msg1.indexOf(",message:")+9,msg1.indexOf(",message:")+9+4).equals("room")){
                            room = msg1.substring(msg1.indexOf(",message:")+9+4,msg1.lastIndexOf(",timestamp:"));
                        } else {
                            payload += msg1.substring(msg1.indexOf(",message:")+9,msg1.lastIndexOf(",timestamp:"));
                        }}
                        break;
                    case "history":
                        System.out.println("History");
                        break;
                    default:
                        //shouldnt ever happen but procs a funny statement if it does for troubleshooting
                        System.out.println("something hapen,\nruh roh gamers");
                        break;
                }
                msg1 = payload;
            } //end of if statement
            //output chat messages
            System.out.println(msg1);
        } // end while loop
    }// end loop
}// end class
