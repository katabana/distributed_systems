import akka.Done;
import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class Z4_DatabaseActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String title;
    private final String path;


    Z4_DatabaseActor(String title, String path) {
        this.title = title;
        this.path = path;
    }

    private double findPrice() throws Exception {
        File file = new File(path);
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("Database \"" + path + "\" is not available.");
            getContext().parent().tell(Done.getInstance(), getSelf());
            return -1;
        }

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] args = line.split(" ");
            if (args[0].equals(title)) {
                if (args.length == 1) {
                    log.info("Wrong database format line: " + line);
                } else
                    return Double.parseDouble(args[1]);
            }
        }
        return -1;
    }

    @Override
    public void preStart() throws Exception {

        double result = findPrice();
        if (result >= 0) {
            getContext().getParent().tell(result, getSelf());
        } else
            getContext().parent().tell(Done.getInstance(), getSelf());

    }

    @Override
    public void postStop() throws Exception {
        log.info("Stopped a searching child " + getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(o -> log.info("received unknown message" + o))
                .build();
    }
}
