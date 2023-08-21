package ru.mediatel.shellclient;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ShellclientApplication {

	public static void main(String[] args) {
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
