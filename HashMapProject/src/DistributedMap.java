import java.io.Serializable;
import java.util.HashMap;


public class DistributedMap implements SimpleStringMap {

    private HashMap<String, String> distributedMap = new HashMap<>();

    public void set(HashMap<String, String> distributedMap) {
        this.distributedMap = distributedMap;
    }

    public HashMap<String, String> getDistributedMap(){
        return distributedMap;
    }

    @Override
    public boolean containsKey(String key) {
        return distributedMap.containsKey(key);
    }

    @Override
    public String get(String key) {
        return distributedMap.get(key);
    }

    @Override
    public String put(String key, String value) {
        return distributedMap.put(key, value);
    }

    @Override
    public String remove(String key) {
        return distributedMap.remove(key);
    }
}
