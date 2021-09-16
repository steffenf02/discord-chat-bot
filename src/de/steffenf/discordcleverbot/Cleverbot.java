package de.steffenf.discordcleverbot;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cleverbot {

    // this is a lazy "port" of a nodejs library to java
    // it uses an api to execute js code because the cleverbot page does some weird client sided stuff.
    // this also stopped working because the api was changed

    private String jsLibraries;

    String xvis = "";

    OkHttpClient client = null;

    public Cleverbot(){
        System.out.println("Setting up HttpClient...");

        client = OkHttpUtil.getUnsafeOkHttpClient();

        System.out.println("Done. Reading jslibs.js now");

        try {
            jsLibraries = new String(Files.readAllBytes(new File("jslibs.js").toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Done. Sending a test request now.");

        ArrayList<String> context = new ArrayList<String>();
        context.add("Hey? \uD83D\uDE02");

        String jsAPIResponse = generatePayload("How are you?", context);

        log("response from gocodeapi: " + jsAPIResponse);

        log("Done. Reply: " + sendRequest(jsAPIResponse, generateXVIS()));

    }

    public static void log(String s){
        System.out.println("CLEVERBOT >> " + s);
    }

    String regex = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]";
    Pattern pattern = Pattern.compile(
            regex,
            Pattern.UNICODE_CHARACTER_CLASS);

    public String generatePayload(String message, ArrayList<String> context){

        ArrayList<String> bruh2 = new ArrayList<>();
        for(String s : context){
            Matcher matcher = pattern.matcher(("Buffer.from(\"" + Base64.getEncoder().encodeToString((s.replace("\"", "").replace("\n", " ")).getBytes()) + "\", \"base64\").toString()"));
            bruh2.add(matcher.replaceAll(""));
        }

        String code = Base64.getEncoder().encodeToString((""+message.replace("\"", "").replace("\n", " ") + ", "+ Arrays.toString(bruh2.toArray())).getBytes());


        return Main.gson.fromJson(executeScript(jsLibraries + ";console.log(generatePayload(Buffer.from(\"" + code + "\", \"base64\").toString().replace(/([\\uE000-\\uF8FF]|\\uD83C[\\uDF00-\\uDFFF]|\\uD83D[\\uDC00-\\uDDFF])/g, ''), "+ Arrays.toString(bruh2.toArray()) + "))"), JsApiResponse.class).data.res;
    }

    public String executeScript(String s) {

        Request req = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "code=" + URLEncoder.encode(s) + "&input=&lang=js"))
                .url("https://removed/run_code.php")
                .build();


        try {
            Response response = client.newCall(req).execute();
            String resp = new String(response.body().bytes());
            response.body().close();
            return resp;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String sendRequest(String payload, String XVIS){
        Request request = new Request.Builder()
                .url("removed") // this used to be cleverbots api
                .header("Cookie", XVIS)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), payload)).build();
        try {
            Response response = client.newCall(request).execute();
            response.body().close();

            String resp = URLDecoder.decode(response.headers().get("cboutput"));

            return resp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }

    public String generateXVIS(){
        // getting the XVIS cookie
        Request request = new Request.Builder()
                .get()
                .url("https://www.cleverbot.com/")
                .build();
        try {
            Response response = client.newCall(request).execute();
            response.body().close();
            return response.headers().get("Set-Cookie");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "null";
    }

}
