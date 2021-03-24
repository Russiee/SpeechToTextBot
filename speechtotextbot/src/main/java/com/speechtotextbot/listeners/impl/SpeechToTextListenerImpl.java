package com.speechtotextbot.listeners.impl;

import com.speechtotextbot.listeners.SpeechToTextListener;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpeechToTextListenerImpl extends ListenerAdapter implements SpeechToTextListener {
    private static final Pattern START_LISTEN_COMMAND = Pattern.compile("!listen start (\\w+)");
    private static final Pattern STOP_LISTEN_COMMAND = Pattern.compile("!listen stop (\\w+)");
    private static final String LISTEN_COMMAND = "!listen";

    @Autowired
    private ConnectionListener connectionListener;
    @Override
    public void onMessageReceived(MessageReceivedEvent messageCreateEvent) {
        if (messageCreateEvent.getMessage().getContentRaw().startsWith(LISTEN_COMMAND)) {
            if (Pattern.matches(START_LISTEN_COMMAND.pattern(), messageCreateEvent.getMessage().getContentRaw())) {
                Matcher matcher = START_LISTEN_COMMAND.matcher(messageCreateEvent.getMessage().getContentRaw());
                if(matcher.matches()) {
                    String channel = matcher.group(1);

                    List<VoiceChannel> voiceChannelList = new ArrayList<>(messageCreateEvent.getJDA().getVoiceChannelsByName(channel, true));
                    if(voiceChannelList.size() > 1) {
                        //Thats weird - exception
                    } else {
                        AudioManager audioManager = messageCreateEvent.getGuild().getAudioManager();
                        audioManager.openAudioConnection(voiceChannelList.get(0));
                        connectionListener.createSpeechToText(messageCreateEvent.getTextChannel());
                        audioManager.setReceivingHandler(connectionListener);
                        audioManager.setSendingHandler(connectionListener);
                    }
                }
            }
        }
    }
}
