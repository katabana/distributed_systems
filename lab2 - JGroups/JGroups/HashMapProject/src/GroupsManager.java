import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;

import java.io.*;
import java.net.InetAddress;
import java.util.List;

//TODO: partycjonowanie - mergowanie <- dokumentacja
//TODO: It is essential that the merge view handling code run on a separate thread if it needs more than a few milliseconds, or else it would block the calling thread.

public class GroupsManager extends ReceiverAdapter {
    private JChannel channel;
    private Client client;

    public void getState(OutputStream output) throws Exception {
        client.getState(output);
    }

    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        client.setState(input);
    }

    @Override
    public void viewAccepted(View view) {
        if(view instanceof MergeView) {
            ViewHandler handler = new ViewHandler(channel, (MergeView) view);
            // requires separate thread as we don't want to block JGroups
            handler.start();

        }
    }

    public void receive(Message msg) {
        System.out.println("received msg from "
                + msg.getSrc() + ": "
                + msg.getObject());
        String line = msg.getObject().toString();
        String[] args = line.split(" ");

        if(line.startsWith("remove")) {
            client.remove(args[1]);
        }
        else if(line.startsWith("put")) {
            client.put(args[1], args[2]);
        }
    }

    private void start() throws Exception {
        System.setProperty("java.net.preferIPv4Stack","true");
        channel = new JChannel(false);
        ProtocolStack stack = new ProtocolStack();

        channel.setProtocolStack(stack);
        stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName("230.0.0.7")))
                .addProtocol(new PING())
                .addProtocol(new MERGE3()) //merge protocol
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL()
                        .setValue("timeout", 12000)
                        .setValue("interval", 3000))
//                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE()); //because STATE_TRANSFER "should not be used for transfer of large states"
        stack.init();
        channel.setReceiver(this);
        client = new Client(channel);
        channel.connect("Cluster");
        channel.getState(null, 10000);
        eventLoop();
        channel.close();
    }

    private void eventLoop() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                System.out.print("> "); System.out.flush();
                String line = in.readLine().toLowerCase();
                String[] args = line.split(" ");
                if(line.startsWith("get"))  {
                    if(args.length == 2)
                        System.out.println("FOUND: " + client.get(args[1]));
                    else
                        System.out.println("Give a key");
                }
                else if (line.startsWith("contains")) {
                    if(args.length == 2)
                        System.out.println("Contains " + args[1] + " " + client.containsKey(args[1]));
                    else
                        System.out.println("Give a key");
                }
                else if (line.startsWith("put") && args.length < 3) {
                    if(args.length == 2)
                        System.out.println("Give a value to put on key " + args[1]);
                    else
                        System.out.println("Give a key and a value");
                }
                else if(line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                }
                else if(!line.isEmpty()){
                    Message msg = new Message(null, null, line);
                    channel.send(msg);
                }
            }
            catch(Exception e) {
                //System.out.println(e.toString());
            }
        }
    }

    private static class ViewHandler extends Thread {
        JChannel ch;
        MergeView view;

        private ViewHandler(JChannel ch, MergeView view) {
            this.ch = ch;
            this.view = view;
        }

        public void run() {
            List<View> subgroups = view.getSubgroups();
            View tmp_view = subgroups.get(0); //first(); // picks the first
            Address local_addr = ch.getAddress();
            if (!tmp_view.getMembers().contains(local_addr)) {
                System.out.println("Not member of the new primary partition (" + tmp_view + "), will re-acquire the state");
                try {
                    ch.getState(local_addr, 30000);
                } catch (Exception ex) {
                    //System.out.println(ex.toString());
                }
            } else {
                System.out.println("Not member of the new primary partition (" + tmp_view + "), will do nothing");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new GroupsManager().start();
    }
}
