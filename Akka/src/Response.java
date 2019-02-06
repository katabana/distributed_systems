import java.io.Serializable;

public class Response implements Serializable {

    private String content;
    private RequestType type;

    Response(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

}
