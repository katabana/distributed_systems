## JGroups

An implementation of a distributed hash map. Applications using it should have the possibility of adding elements to the map and retrieving previously added elements, including retrieving the data by other applications.

The project was to implement the interface:

```java
public interface SimpleStringMap {
    boolean containsKey(String key);

    String get(String key);

    String put(String key, String value);

    String remove(String key);
}

```

An example application presenting the usage of the designed hash map was created. It shows methods included in the interface of the implemented map.
With the CAP theorem taken into consideration, only partition tolerance and availability is guaranteed.

Each instance of the DistributedMap class has its own copy of the distributed map, and synchronization of the states should be done while adding new elements to the map.

For distributed communication between instances, the JGroups library was used:

- in case of creating a new instance of the DistributedMap class, it should get the initial state from the members of the group that it is joining;
- in case of division of nodes group to two partitions, they should maintain independent states,
- in case of merging two partitions, members of one randomly chosen partition should lose their state and get a new one from members from the other partition.


Sources:

http://www.jgroups.org/manual/index.html#StateTransfer
http://www.jgroups.org/manual/index.html#HandlingNetworkPartitions
