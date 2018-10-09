import org.jgroups.JChannel;
import org.jgroups.util.Util;

import java.io.*;
import java.util.HashMap;


public class Client {
    private DistributedMap distributedMap = new DistributedMap();
    private JChannel jChannel;

    public Client(JChannel channel) {
        jChannel = channel;
    }

    public void getState(OutputStream output) throws Exception {
        synchronized(distributedMap) {
            Util.objectToStream(distributedMap.getDistributedMap(), new DataOutputStream(output));
        }
    }

    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        HashMap<String, String> receivedMap = (HashMap<String, String>) Util.objectFromStream(new DataInputStream(input));
        synchronized(distributedMap) {
            distributedMap.set(receivedMap);
        }
    }

    public String remove(String key) {
        //remove from all maps
        String o = distributedMap.get(key);
        System.out.println(o + " has been removed");
        return distributedMap.remove(key);
    }


    public String put(String key, String value) {
        // put to all maps
        System.out.println(value + " has been added");
        return distributedMap.put(key, value);
    }

    public String get(String key) {
        return distributedMap.get(key);
    }

    public boolean containsKey(String key) {
        return distributedMap.containsKey(key);
    }

}
