package com.speechtotextbot.listeners.impl;

import net.dv8tion.jda.api.audio.*;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class ConnectionListener implements AudioReceiveHandler {


    @Override
    public void handleUserAudio(UserAudio userAudio) {
        userAudio.getUser()
                .getJDA()
                .getTextChannelsByName("test", true)
                .get(0)
                .sendMessage(userAudio.getUser().getName() + ": is saying: \n" + new String(userAudio.getAudioData(1)));
    }


    @Override
    public boolean canReceiveUser() {
        return true;
    }
}
