package com.speechtotextbot.listeners.impl;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.util.DateUtils;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.*;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.speech_to_text.v1.websocket.RecognizeCallback;
import net.dv8tion.jda.api.audio.*;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.gson.internal.$Gson$Types.arrayOf;

@Component
public class ConnectionListener implements AudioReceiveHandler, AudioSendHandler {
    AudioFormat OUTPUT_FORMAT = new AudioFormat(48000.0f, 16, 2, true, true);

    @Autowired
    private Environment env;

    private static boolean HANDLING = false;
    private TextChannel textChannel;
    private List<byte[]> voiceData = new ArrayList<>();
    private int afkTimer;
    private SpeechToText service;
    private Date startTime = new Date();
    private boolean canProvide = true;

    public void createSpeechToText(TextChannel textChannel) {
        this.textChannel = textChannel;
        IamAuthenticator iamAuthenticator = new IamAuthenticator(env.getProperty("IBM_API"));
        service = new SpeechToText(iamAuthenticator);
        service.setServiceUrl("https://api.eu-gb.speech-to-text.watson.cloud.ibm.com/instances/08053cb8-8c94-45d9-acaf-8486cf1ef65d");
    }

    @Override
    public void handleUserAudio(@Nonnull UserAudio combinedAudio) {
        voiceData.add(combinedAudio.getAudioData(1.0f));
    }

    @Override
    public boolean canReceiveUser() {
        if (!HANDLING) {
            afkTimer++;
            if (voiceData.size() >= 20) {
                HANDLING = true;
                sendMessage();
                afkTimer = 0;
                HANDLING = false;
            }
        }
        return !HANDLING;
    }

    private void sendMessage() {
        transcribe(voiceData.stream()
                .collect(
                        () -> new ByteArrayOutputStream(),
                        (b, e) -> {
                            try {
                                b.write(e);
                            } catch (IOException e1) {
                                throw new RuntimeException(e1);
                            }
                        },
                        (a, b) -> {
                        }).toByteArray());
        voiceData.clear();
//        String toSend = new String(decodedData);
//        int previousIndex = -1;
//        while (toSend.length() > 0) {
//            if (toSend.length() < 1900) {
//                user.getJDA()
//                        .getTextChannelsByName("test", true)
//                        .get(0)
//                        .sendMessage(user.getName() + ": is saying: \n" + toSend).queue();
//                toSend = "";
//            } else {
//                int newIndex = previousIndex + 1900;
//                if (newIndex > toSend.length()) {
//                    newIndex = toSend.length();
//                }
//                String substring = toSend.substring(previousIndex + 1, newIndex);
//                previousIndex = newIndex;
//                user.getJDA()
//                        .getTextChannelsByName("test", true)
//                        .get(0)
//                        .sendMessage(user.getName() + ": is saying: \n" + substring).queue();
//            }
//        }
    }

    private void transcribe(byte[] bytes) {
        // Signed PCM AudioFormat with 16kHz, 16 bit sample size, mono
        int sampleRate = 48000;

        AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(bytes), OUTPUT_FORMAT, bytes.length);

        RecognizeWithWebsocketsOptions options = new RecognizeWithWebsocketsOptions.Builder()
                .audio(audio)
                .interimResults(true)
                .model("en-GB_BroadbandModel")
                .wordConfidence(true)
                //.contentType(HttpMediaType.AUDIO_WAV)
                .contentType(HttpMediaType.AUDIO_PCM + "; rate=" + sampleRate + "; endianness=big-endian")
                .build();

        service.recognizeUsingWebSocket(options, new BaseRecognizeCallback() {
            @Override
            public void onTranscription(SpeechRecognitionResults speechRecognitionResults) {
                System.out.println(speechRecognitionResults);
                if (speechRecognitionResults.getResults().size() > 0) {
                    List<SpeechRecognitionResult> results = speechRecognitionResults.getResults();
                    textChannel.sendMessage(results.stream()
                            .map(SpeechRecognitionResult::getAlternatives)
                            .flatMap(Collection::stream)
                            .map(SpeechRecognitionAlternative::getTranscript)
                            .collect(Collectors.joining("\n"))).queue();
                }
            }
        });
    }

    @Override
    public boolean canProvide() {
        return canProvide;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        // send the silence only for 5 seconds
        if (((new Date().getTime() - startTime.getTime()) / 1000) > 5) {
            canProvide = false;
        }

        return ByteBuffer.wrap(voiceData.stream()
                .collect(
                        () -> new ByteArrayOutputStream(),
                        (b, e) -> {
                            try {
                                b.write(e);
                            } catch (IOException e1) {
                                throw new RuntimeException(e1);
                            }
                        },
                        (a, b) -> {
                        }).toByteArray());
        //(Byte.valueOf("0xF8"), Byte.valueOf("0xFF"), Byte.valueOf("0xFE"));
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
