## Zookeeper

An example application using Zookeeper. 

The functionalities provided by the application are:
- when a znode called "znode_testowy" is created, an external application is run (any, specified in the command line),
- when the "znode_testowy" znode is deleted, the external application is stopped,
- when a new descendant of the "znode_testowy" znode is added, a graphic information about the current number of its descendants is shown,
- it is possible to preview the whole structure of the "znode_testowy" znode's tree.


The application works in the "Replicated ZooKeeper" environment.

