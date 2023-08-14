package ru.mediatel.shellclient;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class ShellclientApplication {

	public static void main(String[] args) {
//		SpringApplication.run(ShellclientApplication.class, args);
		SpringApplication application = new SpringApplication(ShellclientApplication.class);
		application.setLogStartupInfo(false);
		application.setBannerMode(Banner.Mode.OFF);
		application.run(args);
	}

	@PostConstruct
	public void init() {
		System.out.println("Type 'help' to display commands list");
	}
}
