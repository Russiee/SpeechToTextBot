/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.speechtotextbot.listeners.impl;

import net.dv8tion.jda.api.entities.TextChannel;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * This class primarily controls the GUI for this application. Most of the code relevant to starting and working
 * with our streaming API can be found in TranscribeStreamingClientWrapper.java, with the exception of some result
 * parsing logic in this classes method getResponseHandlerForWindow()
 */
public class WindowController {

    private TranscribeStreamingClientWrapper client;
    private CompletableFuture<Void> inProgressStreamingRequest;
    private TextChannel textChannel;

    public WindowController(TextChannel textChannel) {
        this.textChannel = textChannel;
        client = new TranscribeStreamingClientWrapper();
        initializeWindow();
    }

    private void startTranscriptionRequest() {
        if (inProgressStreamingRequest == null) {
            inProgressStreamingRequest = client.startTranscription(getResponseHandlerForWindow());
        }
    }

    private void initializeWindow() {
        startTranscriptionRequest();
    }


    /**
     * A StartStreamTranscriptionResponseHandler class listens to events from Transcribe streaming service that return
     * transcriptions, and decides what to do with them. This example displays the transcripts in the GUI window, and
     * combines the transcripts together into a final transcript at the end.
     */
    private StreamTranscriptionBehavior getResponseHandlerForWindow() {
        return new StreamTranscriptionBehavior() {

            //This will handle errors being returned from AWS Transcribe in your response. Here we just print the exception.
            @Override
            public void onError(Throwable e) {
                System.out.println(e.getMessage());
                Throwable cause = e.getCause();
                while (cause != null) {
                    System.out.println("Caused by: " + cause.getMessage());
                    Arrays.stream(cause.getStackTrace()).forEach(l -> System.out.println("  " + l));
                    if (cause.getCause() != cause) { //Look out for circular causes
                        cause = cause.getCause();
                    } else {
                        cause = null;
                    }
                }
                System.out.println("Error Occurred: " + e);
            }

            /*
            This handles each event being received from the Transcribe service. In this example we are displaying the
            transcript as it is updated, and when we receive a "final" transcript, we append it to our finalTranscript
            which is returned at the end of the microphone streaming.
             */
            @Override
            public void onStream(TranscriptResultStream event) {
                List<Result> results = ((TranscriptEvent) event).transcript().results();
                if(results.size()>0) {
                    Result firstResult = results.get(0);
                    if (firstResult.alternatives().size() > 0 && !firstResult.alternatives().get(0).transcript().isEmpty()) {
                        String transcript = firstResult.alternatives().get(0).transcript();
                        if(!transcript.isEmpty()) {
                            System.out.println(transcript);
                            //textChannel.sendMessage(transcript).queue();
                        }
                    }

                }
            }

            /*
            This handles the initial response from the AWS Transcribe service, generally indicating the streams have
            successfully been opened. Here we just print that we have received the initial response and do some
            UI updates.
             */
            @Override
            public void onResponse(StartStreamTranscriptionResponse r) {
                System.out.println(String.format("=== Received Initial response. Request Id: %s ===", r.requestId()));
            }

            /*
            This method is called when the stream is terminated without error. In our case we will use this opportunity
            to display the final, total transcript we've been aggregating during the transcription period and activates
            the save button.
             */
            @Override
            public void onComplete() {
                System.out.println("=== All records streamed successfully ===");
            }
        };
    }

}
