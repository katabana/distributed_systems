import akka.Done;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.restart;

public class Z4_BookstoreActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private boolean writingToFile = false;

    private int id = 0;


    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    System.out.println("Bookstore received: " + s);
                })
                .match(Integer.class, i -> {
                    System.out.println("Bookstore received: " + i.toString());
                })
                .match(Request.class, i -> {
                    RequestType type = i.getType();
                    String title = i.getTitle();
                    ActorRef sender = getSender();

                    if (type == RequestType.FIND) {
                        System.out.println("Find: " + title);
                        searchForPrice(title, sender);
                    } else if (type == RequestType.ORDER) {
                        System.out.println("Order: " + title);
                        makeOrder(title, sender);
                    } else if (type == RequestType.STREAM) {
                        System.out.println("Stream: " + i.getTitle());
                        streamBook(title, sender);
                    } else if (type == RequestType.UNLOCK) {
                        writingToFile = false;
                    } else if (type == RequestType.LOCK) {
                        if(writingToFile) {
                            ActorSystem system = getContext().getSystem();
                            system.scheduler().scheduleOnce(
                                    FiniteDuration.create(50, TimeUnit.MILLISECONDS),
                                    getSelf(), i, system.dispatcher(), getSender());
                        } else {
                            writingToFile = true;
                            getSender().tell(new Request(RequestType.LOCK, ""), getSelf());
                        }
                    } else{
                        log.info("Wrong request");
                        getSender().tell("Wrong request. Request type is not supported.", getSelf());
                    }
                })
                .match(Response.class, s -> {
                    context().stop(getSender());
//                    context().actorSelection(s.getReceiver()).tell(s, getSelf());
                })
                .matchAny(o -> log.info("received unknown message" + o))
                .build();
    }

    private void searchForPrice(String title, ActorRef sender) {
        context().actorOf(Props.create(Z4_SearchActor.class, title, sender), "search_actor" + Integer.toString(++id));
    }

    private void makeOrder(String title, ActorRef sender) {
        context().actorOf(Props.create(Z4_OrderActor.class, title, sender), "order_actor" + Integer.toString(++id));
    }

    private void streamBook(String title, ActorRef sender) {
        context().actorOf(Props.create(Z4_StreamActor.class, title, sender), "stream_actor" + Integer.toString(++id));
    }


    private static SupervisorStrategy strategy
            = new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder
            .match(IOException.class, e -> SupervisorStrategy.restart())
            .match(FileNotFoundException.class, e -> SupervisorStrategy.stop())
            .matchAny(o -> restart())
            .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
