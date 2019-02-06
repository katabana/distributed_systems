import akka.Done;
import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.ByteString;


public class Z4_ClientActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final String path = "akka.tcp://server_system@127.0.0.1:3552/user/bookstore_actor";


    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, s -> {
                    getContext().actorSelection(path).tell(s, getSelf());
                })
                .match(Response.class, s -> {
                    System.out.println(s.getContent());
                })
                .match(Double.class, i -> {
                    System.out.println("Received: " + i.toString());
                })
                .match(Done.class, i -> {
                    System.out.println("Finished.");
                })
                .match(String.class, s -> System.out.println(s))
                .match(ByteString.class, s -> {
                    System.out.println("B " + s);
                })
                .matchAny(o -> log.info("received unknown message" + o))
                .build();
    }
}
