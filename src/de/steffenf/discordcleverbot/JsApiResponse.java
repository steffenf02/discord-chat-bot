package de.steffenf.discordcleverbot;

public class JsApiResponse {

    public int status;

    public ResponseData data;

    public static class ResponseData{
        public long comp;
        public String res;
        public long[] timings;
    }

}
