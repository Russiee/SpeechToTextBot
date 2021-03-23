package com.speechtotextbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class SpeechToTextBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpeechToTextBotApplication.class, args);
	}

	@Autowired
	private Environment env;

	@Bean
	@ConfigurationProperties("discord-api")
	public DiscordApi discordApi() {
		System.out.println("Initialising api");
		String token = env.getProperty("TOKEN");
		System.out.println(token);
		DiscordApiBuilder builder = new DiscordApiBuilder().setToken(token).setAllNonPrivilegedIntents();
		DiscordApi api = builder.login().join();
		System.out.println("Initialising listeners");
		return api;
	}
}
