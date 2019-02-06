import java.io.Serializable;

public class Request implements Serializable {

    private RequestType type;
    private String title;

    public Request(RequestType type, String title) {
        this.type = type;
        this.title = title;
    }

    public RequestType getType() {
        return this.type;
    }

    public String getTitle(){
        return this.title;
    }

    public String toString() {
        return String.format("Request.%s for \"%s\"", type.toString(), title);
    }

}
