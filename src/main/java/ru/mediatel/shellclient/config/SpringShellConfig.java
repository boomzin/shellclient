package ru.mediatel.shellclient.config;

import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class SpringShellConfig {

    @Bean
    public ShellHelper shellHelper(Terminal terminal) {
            return new ShellHelper(terminal);
    }


}
