import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Z4_server {

    public static void main(String[] args) {
        // config
        File configFile = new File("remote_app2.conf");
        Config config = ConfigFactory.parseFile(configFile);

        final ActorSystem system = ActorSystem.create("server_system", config);
        final ActorRef bookstoreActor = system.actorOf(Props.create(Z4_BookstoreActor.class), "bookstore_actor");


        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = null;
            try {
                line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (line.equals("q")) {
                break;
            }
//            bookstoreActor.tell("", null);
        }

        system.terminate();
    }

}
