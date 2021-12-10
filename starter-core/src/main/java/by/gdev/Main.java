package by.gdev;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.apache.commons.logging.impl.SimpleLog;

import com.beust.jcommander.JCommander;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import by.gdev.component.Starter;
import by.gdev.model.StarterAppConfig;
import by.gdev.subscruber.ConsoleSubscriber;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
	public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	//Can get encoding from args and it is too java.net.preferIPv4Stack
	public static Charset charset = StandardCharsets.UTF_8;

	public static void main(String[] args) throws Exception {
		log.info("i");
		log.trace("tr");
		System.out.println(123);
		boolean flag = true;
		System.setProperty("java.net.preferIPv4Stack", String.valueOf(flag));
		EventBus eventBus = new EventBus();
		eventBus.register(new ConsoleSubscriber());
		StarterAppConfig starterConfig = StarterAppConfig.DEFAULT_CONFIG;
		JCommander.newBuilder().addObject(starterConfig).build().parse(args);
		try {
			Starter s = new Starter(eventBus, starterConfig);
			s.collectOSInfo();
			s.validateEnvironmentAndAppRequirements();
			s.prepareResources();
			s.runApp();
		} catch (Throwable t) {
//			log.error("Error", t);
			System.exit(-1);
		}
	}
}