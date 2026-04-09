import java.util.*;

public class ChatRoom {
    //name of room
    String name;
    //list of users
    List<Integer> users = Collections.synchronizedList(new ArrayList<Integer>());
    
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
}
