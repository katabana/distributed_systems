import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class Z4_OrderActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String title;
    private final ActorRef sender;
    private final String path = "Databases/orders";

    public Z4_OrderActor(String title, ActorRef sender) {
        this.title = title;
        this.sender = sender;
    }

    // 25 i 51

    @Override
    public void preStart() throws Exception {
        getContext().getParent().tell(new Request(RequestType.LOCK, ""), getSelf());
    }

    private void saveOrder() throws Exception {
        String msg = String.format("The book \"%s\" has been ordered.", title);
        try {
            Files.write(Paths.get(path), (title + "\n").getBytes(), StandardOpenOption.APPEND);
        } catch (NoSuchFileException e) {
            getContext().stop(getSelf());
            msg = String.format("Database problem. The order for book \"%s\" cannot be finished.", title);
        }
        getContext().getParent().tell(new Request(RequestType.UNLOCK, ""), getSelf());
        sendResult(msg);
    }

    private void sendResult(String msg) {
        log.info(msg);
        sender.tell(msg, getSelf());
        getContext().stop(getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, s -> {
                    RequestType type = s.getType();
                    if (type == RequestType.LOCK) {
                        saveOrder();
                    } else {
                        sender.tell("Wrong request type. " + s.toString(), getSelf());
                        getContext().stop(getSelf());
                    }
                })
                .match(Done.class, s -> context().stop(getSelf()))
                .matchAny(o -> log.info("received unknown message " + o))
                .build();
    }
}
