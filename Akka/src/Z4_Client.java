import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Z4_Client {

    public static void main(String[] args) {

        File configFile;
        if (args.length >= 2) {
            configFile = new File(args[1]);
        } else {
            // config
            configFile = new File("remote_app.conf");
        }
        Config config = ConfigFactory.parseFile(configFile);

        // create actor system & actors
        final ActorSystem system = ActorSystem.create("client_system", config);
        final ActorRef clientActor = system.actorOf(Props.create(Z4_ClientActor.class), "client_actor");

        // interaction
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line;
            try {
                line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            if (line.equals("q")) {
                break;
            } else if (line.startsWith("f")) {
                sendRequest(RequestType.FIND, line, clientActor);
            } else if (line.startsWith("o")) {
                sendRequest(RequestType.ORDER, line, clientActor);
            } else if (line.startsWith("s")) {
                sendRequest(RequestType.STREAM, line, clientActor);
            } else {
                System.out.println("Invalid command: " + line);
                // printHelp();
            }
        }

        system.terminate();
    }

    private static void sendRequest(RequestType type, String line, ActorRef clientActor) {
        String title = line.split(" ")[1];
        clientActor.tell(new Request(type, title), null);
    }
}
