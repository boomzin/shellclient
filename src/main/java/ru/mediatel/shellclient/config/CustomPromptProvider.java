package ru.mediatel.shellclient.config;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;


@Component
public class CustomPromptProvider implements PromptProvider {
    private  String shellPrefix = "n2jss7 client ==> ";

    public String getShellPrefix() {
        return shellPrefix;
    }

    public void setShellPrefix(String shellPrefix) {
        this.shellPrefix = shellPrefix;
    }

    @Override
    public AttributedString getPrompt()
    {
        return new AttributedString(shellPrefix, AttributedStyle.DEFAULT.background(AttributedStyle.GREEN));
    }
}
