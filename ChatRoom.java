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
        String fullHistory = "type:history,messages:";
        String[] messages = history.toArray(new String[0]);
        for(String s : messages){
            fullHistory += s + "\n";
        }
        return fullHistory;
    }

    //synchronized method to return the size of the history
    public synchronized int historySize(){
        return history.size();
    }

}
