import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ChatRoom {
    //name of room
    String name;
    //list of users
    List<Integer> users = Collections.synchronizedList(new ArrayList<Integer>());
    ConcurrentLinkedDeque<String> history = new ConcurrentLinkedDeque<String>();
    
    //comparison method: checks if o is a chatroom, if not, return false, otherwise, return whether or not the name is equal
    @Override
    public boolean equals(Object o){
        if(o instanceof ChatRoom){
            ChatRoom object = (ChatRoom) o;
            return object.name.equals(this.name); 
        } else {
            return false;
        }
    }

    //constructor
    public ChatRoom(String name){
        this.name = name;
    }

    //synchronized editors for the list of users
    public synchronized void add(int i){
        users.add(i);
    }

    public synchronized void remove(int i){
        users.remove(users.indexOf(i));
    }
    //synchronized getter to return the list of users as an int array
    public synchronized int[] users(){
        int[] list = new int[users.size()];
        for (int i = 0; i<users.size(); i++) {
            list[i] = users.get(i);
        }
        return list;
    }
    //synchronized getter for name
    public synchronized String name(){
        return name;
    }

    //synchronized editor for history queue
    public synchronized void historyAdd(String message){
        //add message to tail of deque
        history.add(message);
        if(history.size()==20){
            //if size is larger than 20, remove first element of deque to make room
            history.removeFirst();
        }
    }

    //synchronized method to return the history of the room
    public synchronized String historyGet(){
        //declare
        String date;
        String type;
        String addedMsg;
        String fullHistory = "type:history,messages:";
        String[] messages = history.toArray(new String[0]);
        for(String s : messages){
            //get the type and the date since those are the ones that are always importantand always in the same spot
            date = s.substring(s.lastIndexOf(",timestamp:")+11);
            fullHistory += date + " :: ";
            
            //the message always starts with "type:" so we just skip to index 5
            type = s.substring(5,s.indexOf(','));
            //if the message is of type text, get the message from text, otherwise, get it from message
            if(type.equals("text")){
                fullHistory += "[" + this.name +"] " + s.substring(s.indexOf(",nickname:")+10,s.lastIndexOf(",userID:"))+": ";
                addedMsg = s.substring(s.indexOf(",text:")+6,s.lastIndexOf(",timestamp:"));
            } else {
                addedMsg = s.substring(s.indexOf(",message:")+9,s.lastIndexOf(",timestamp:"));
            }
            fullHistory += addedMsg + "\n";
        }
        return fullHistory;
    }

    //synchronized method to return the size of the history
    public synchronized int historySize(){
        return history.size();
    }

}
