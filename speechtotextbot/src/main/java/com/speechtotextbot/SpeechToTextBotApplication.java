package com.speechtotextbot;

import com.speechtotextbot.listeners.SpeechToTextListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.security.auth.login.LoginException;

@SpringBootApplication
public class SpeechToTextBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpeechToTextBotApplication.class, args);
	}

	@Autowired
	private Environment env;

	@Autowired
	private SpeechToTextListener speechToTextListener;

	@Bean
	@ConfigurationProperties("discord-api")
	public JDA discordApi() throws LoginException{
		System.out.println("Initialising api");
		String token = env.getProperty("TOKEN");
		JDABuilder builder = JDABuilder.createDefault(token);
		// Disable parts of the cache
		builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
		// Enable the bulk delete event
		builder.setBulkDeleteSplittingEnabled(false);
		// Set activity (like "playing Something")
		builder.setActivity(Activity.listening("Speech to Texxt"));
		builder.addEventListeners(speechToTextListener);
		System.out.println(token);
		System.out.println("Initialising listeners");
		return builder.build();
	}
}
