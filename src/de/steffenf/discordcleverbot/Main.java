package de.steffenf.discordcleverbot;

import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends ListenerAdapter {

    public static Gson gson = new Gson();
    public static Cleverbot cb;
    public static JDA jda;
    public static TextChannel logChannel;


    public static ConcurrentHashMap<String, Long> ratelimits = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, ArrayList<String>> histories = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, String> xvis = new ConcurrentHashMap<>();

    boolean ghost = false;

    public static void main(String[] args) {


        // read token from env
        String token;
        try {
            token = System.getenv("token");
        } catch (Exception e) {
            log("Error getting token:");
            e.printStackTrace();
            return;
        }

        if(token == null || token.length() == 0){
            /*log("token env var not found, checking for .env file");
            if(new File(".env").exists()){
                try {
                    List<String> lines = Files.readAllLines(new File(".env").toPath());
                    for(String line : lines){
                        String[] kv = line.split("=", 1);
                        String key = kv[0];
                        String value = kv[0];
                        if(key.equalsIgnoreCase("token")){
                            token = value;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error reading .env");
                    e.printStackTrace();
                    return;
                }
            }else{*/
                log("Error reading token from ENV. Are you sure the 'token'-variable is set?");
                return;
            //}

        }

        // not the most elegant object oriented approach but it works
        cb = new Cleverbot();

        try {
            jda = new JDABuilder(token)
                    .addEventListeners(new Main())
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        if(ghost){
            return;
        }
        new Thread(()->{ // make it not crash/hang the event loop if something goes wrong
            if (event.getMember() == null || event.getMember().getUser().isBot()) {
                return;
            }

            if (!event.getChannel().getName().equalsIgnoreCase("cleverbot")) {
                return;
            }

            if (ratelimits.containsKey(event.getMember().getId())) {
                if (ratelimits.get(event.getMember().getId()) > System.currentTimeMillis() - 2000) {
                    return;
                }
            }
            ratelimits.put(event.getMember().getId(), System.currentTimeMillis());

            event.getChannel().sendTyping().queue(); // tell the user that the bot is processing his message

            log("(%server% in %channel%) - %user%: "
                    .replace("%user%", event.getMember().getUser().getAsTag())
                    .replace("%server%", event.getGuild().getName() + "/" + event.getGuild().getId())
                    .replace("%channel%", event.getChannel().getName() + "/" + event.getChannel().getId())
                    + event.getMessage().getContentRaw());

            String response = cb.sendRequest(
                    cb.generatePayload(
                            event.getMessage().getContentRaw(),
                            getContextForChannel(event.getChannel())),
                    getXVISForChannel(event.getChannel()));

            Cleverbot.log("Replying to " + event.getMember().getUser().getAsTag() + ": " + response);
            if(!response.equalsIgnoreCase("error")) // again, not the most elegant way
            event.getMessage().getChannel().sendMessage(response).queue();

            try {
                if (!histories.containsKey(event.getChannel().getId())) {
                    ArrayList<String> channelHistory = new ArrayList<>(6);
                    channelHistory.add(event.getMessage().getContentRaw());
                    histories.put(event.getChannel().getId(), channelHistory);
                } else {
                    ArrayList<String> channelHistory = histories.get(event.getChannel().getId());
                    if (channelHistory.size() > 5) {
                        channelHistory.remove(5);
                    }
                    channelHistory.add(event.getMessage().getContentRaw());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
        logChannel.sendMessage(
                "joined guild: %name% (%id%) with %members% members"
                .replace("%name%", event.getGuild().getName())
                .replace("%id%", event.getGuild().getId())
                .replace("%members%", String.valueOf(event.getGuild().getMemberCount()))
        ).queue();
        jda.getPresence().setActivity(Activity.watching(jda.getUsers().size() + " smooth brains"));
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        logChannel = jda.getGuildById("364882201751322624").getTextChannelById("715878549227307099");
        logChannel.sendMessage("ready to serve with %total% guilds and %users% users"
                .replace("%total%", String.valueOf(event.getGuildTotalCount()))
                .replace("%users%", String.valueOf(jda.getUsers().size()))
        ).queue();

        jda.getPresence().setActivity(Activity.watching(jda.getUsers().size() + " users"));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        logChannel.sendMessage(
                "left guild: %name% (%id%) with %members% members"
                .replace("%name%", event.getGuild().getName())
                .replace("%id%", event.getGuild().getId())
                .replace("%members%", String.valueOf(event.getGuild().getMemberCount()))
        ).queue();
    }

    public static void log(String s){
        System.out.println("DISCORD >> " + s);
    }

    private String getXVISForChannel(TextChannel channel) {

        if (!xvis.containsKey(channel.getId())) {
            xvis.put(channel.getId(), cb.generateXVIS());
        }

        return xvis.get(channel.getId());

    }

    private ArrayList<String> getContextForChannel(TextChannel channel) {
        if(histories.containsKey(channel.getId())){
            ArrayList<String> historyClone = (ArrayList<String>)histories.get(channel.getId()).clone();
            Collections.reverse(historyClone);
            return historyClone;
        }else{
            return new ArrayList<>(); // return empty history
        }
    }

}
