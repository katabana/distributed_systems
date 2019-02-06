import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

public class DataMonitor implements Watcher, StatCallback {

    ZooKeeper zk;

    String znode;

    Watcher chainedWatcher;

    boolean dead;

    DataMonitorListener listener;

    byte prevData[];

    int lastChildren = 0;


    public DataMonitor(ZooKeeper zk, String znode, Watcher chainedWatcher,
                       DataMonitorListener listener) {
        this.zk = zk;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
        // Get things started by checking if the node exists. We are going
        // to be completely event driven
        zk.exists(znode, true, this, null);
        //
        zk.getChildren(znode, true, childCallback, null);
    }

    /**
     * Other classes use the DataMonitor by implementing this method
     */
    public interface DataMonitorListener {
        /**
         * The existence status of the node has changed.
         */
        void exists(byte data[]);

        void getChildren() throws KeeperException, InterruptedException;

        /**
         * The ZooKeeper session is no longer valid.
         *
         * @param rc
         *                the ZooKeeper reason code
         */
        void closing(int rc);
    }

    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
                case SyncConnected:
                    // In this particular example we don't need to do anything
                    // here - watches are automatically re-registered with
                    // server and any watches triggered while the client was
                    // disconnected will be delivered (in order of course)
                    break;
                case Expired:
                    // It's all over
                    dead = true;
                    listener.closing(KeeperException.Code.SessionExpired);
                    break;
            }
        } else if (event.getType() == Event.EventType.NodeChildrenChanged){
            try {
                Stat stat = zk.exists(path, true);
                if(stat != null) {
                    zk.getChildren(znode, true, childCallback, null);
                    zk.getChildren(path, true);
                    watchChildren(path);
                }
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (event.getType() == Event.EventType.NodeDeleted && path != null && !path.equals(znode)) {
            try {
                zk.exists(path, true);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if (path != null && path.equals(znode)) {
                // Something has changed on the node, let's find out
                try {
                    zk.exists(znode, true, this, null);
                    zk.getChildren(znode, true, childCallback, null);
                    watchChildren(znode);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    zk.exists(path, true);
                    zk.getChildren(path, true);
                    watchChildren(path);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }

    public void watchChildren(String path) throws KeeperException, InterruptedException {
        if (zk.exists(path, false) == null)
            return;
        List<String> children = zk.getChildren(path, false);
        for (String child : children) {
            String pathChild = path.concat("/" + child);
            if(zk.exists(pathChild, false) != null)
                zk.getChildren(pathChild, true);
        }
    }

    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (rc) {
            case Code.Ok:
                exists = true;
                break;
            case Code.NoNode:
                exists = false;
                break;
            case Code.SessionExpired:
            case Code.NoAuth:
                dead = true;
                listener.closing(rc);
                return;
            default:
                // Retry errors
                zk.exists(znode, true, this, null);
                return;
        }

        byte b[] = null;
        if (exists) {
            try {
                b = zk.getData(znode, false, null);
            } catch (KeeperException e) {
                // We don't need to worry about recovering now. The watch
                // callbacks will kick off any exception handling
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }
        if ((b == null && b != prevData)
                || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;
        }

    }


    ChildrenCallback childCallback = new ChildrenCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, List<String> children) {
            // Process the result of the exists call
            boolean exists;
            switch (rc) {
                case Code.Ok:
                    exists = true;
                    break;
                case Code.NoNode:
                    exists = false;
                    break;
                case Code.SessionExpired:
                case Code.NoAuth:
                    dead = true;
                    listener.closing(rc);
                    return;
                default:
                    try {
                        zk.getChildren(znode, true, new Stat()); //znode, true, childCallback, null);
                    } catch (KeeperException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return;
            }
            if(exists) {
                try {
                    int previous = lastChildren;
                    Stat stat = zk.exists(znode, false);
                    int count = countChildren();
                    if(stat != null && previous < count)
                        System.out.println("Children: " + count);
                    listener.getChildren();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("znode_testowy not available");
            }
        }

        public void processResult(int rc, String path, Object ctx, Stat stat) {
            // Process the result of the exists call
            System.out.println("Process Result in child");
        }
    };

    public int countChildren() throws KeeperException, InterruptedException {
        Stat stat = zk.exists(znode, false);
        if (stat != null) {
            int children = countChildrenofNode(znode);
            stat = zk.exists(znode, false);
            if(stat != null)
                lastChildren = children;
                return children;
        }
        return 0;
    }

    public int countChildrenofNode(String node) {
        try {
            List<String> children = zk.getChildren(node, false);
            int count = children.size();
            for (String child : children) {
                count += countChildrenofNode(node.concat("/" + child));
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}