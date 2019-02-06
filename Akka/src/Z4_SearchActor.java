import akka.Done;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import akka.stream.impl.StreamSupervisor;
import scala.concurrent.duration.Duration;

import java.io.FileNotFoundException;
import java.io.IOException;

import static akka.actor.SupervisorStrategy.restart;


public class Z4_SearchActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final String title;
    private final ActorRef sender;
    private String message;
    private int activeActors = 2;


    public Z4_SearchActor(String title, ActorRef sender) {
        this.title = title;
        this.sender = sender;
        this.message = String.format("No price found for \"%s\"", title);
    }


    @Override
    public void preStart() throws Exception {
        context().actorOf(Props.create(Z4_DatabaseActor.class, title, "Databases/db1"), "search_actor1");
        context().actorOf(Props.create(Z4_DatabaseActor.class, title, "Databases/db2"), "search_actor2");
    }


    @Override
    public void aroundPostStop() {
        sendResponse(message);
        getContext().stop(getSelf());
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Double.class, s -> {
                    String msg = String.format("The \"%s\" costs: %.2f", title, s);
                    System.out.println(msg);
//                    sendResponse(msg);
                    setMessage(msg);
                    getContext().stop(getSelf());
                })
                .match(Done.class, s -> {
                    getContext().stop(getSender());
                    activeActors--;
                    if (activeActors == 0) {
                        getContext().stop(getSelf());
                    }
                })
                .matchAny(o -> log.info("received unknown message " + o))
                .build();
    }

    private void setMessage(String msg) {
        message = msg;
    }

    private void sendResponse(String msg) {
//        sender.tell(new Response(sender.path().toSerializationFormat(), message), getSelf());
        sender.tell(new Response(message), getSelf());
    }

    private static SupervisorStrategy strategy
            = new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder
            .match(IOException.class, e -> SupervisorStrategy.restart())
            .match(FileNotFoundException.class, e -> {
                System.out.println("Database is not available");
                return SupervisorStrategy.stop();
            })
            .matchAny(o -> restart())
            .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

}
