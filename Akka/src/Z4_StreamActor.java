import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Framing;
import akka.util.ByteString;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class Z4_StreamActor extends AbstractActor{

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final String title;
    private final ActorRef sender;

    public Z4_StreamActor(String title, ActorRef sender) {
        this.title = title;
        this.sender = sender;
    }

    @Override
    public void preStart() throws Exception {

        final Materializer materializer = ActorMaterializer.create(getContext().getSystem());
        final Path file = Paths.get("Books/" + title);
        if(Files.exists(file)) {
            Sink<ByteString, CompletionStage<Done>> printlnSink = Sink.foreach(chunk -> System.out.println(chunk));

            FileIO.fromPath(file).via(Framing.delimiter(ByteString.fromArray("\n".getBytes()), 256)
                    .map(a -> a.utf8String()))
                    .throttle(1, FiniteDuration.create(1, TimeUnit.SECONDS), 1, ThrottleMode.shaping())
                    .runWith(printlnSink.actorRef(sender, Done.getInstance()), materializer);
        } else {
            log.info(String.format("The file %s does not exist.", title));
            sender.tell(String.format("The book \"%s\" is not available for reading currently", title), getSelf());
        }
        getContext().stop(getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(o -> log.info("received unknown message " + o))
                .build();
    }
}
