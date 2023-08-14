package ru.mediatel.shellclient.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;
import org.springframework.util.ResourceUtils;
import ru.mediatel.shellclient.client.ClientHandler;
import ru.mediatel.shellclient.client.NettyClient;
import ru.mediatel.shellclient.config.CustomPromptProvider;
import ru.mediatel.shellclient.config.ShellHelper;

import java.io.*;

@ShellComponent
@ConfigurationProperties(prefix="jss7-shell")
public class ClientCommand {
    private final String STANDARD_PROMPT = "n2jss7 client ==> ";
    private final String CONNECTING_GREETING = "Try connect";
    private final String CONNECTING_ESTABLISHED = "Connection established successfully";
    private boolean connected;
    private final NettyClient nettyClient;
    private final CustomPromptProvider promptProvider;
    private final ClientHandler clientHandler;
    private final ShellHelper shellHelper;
    @Value("${connectionTimeOut}")
    private int connectionTimeOut;


    public ClientCommand(NettyClient nettyClient, CustomPromptProvider promptProvider, ClientHandler clientHandler, ShellHelper shellHelper) {
        this.nettyClient = nettyClient;
        this.promptProvider = promptProvider;
        this.clientHandler = clientHandler;
        this.shellHelper = shellHelper;
    }

    protected void printHelp(String commandLine) {

        String fileName = "help/" + commandLine + ".txt";
        BufferedReader helpInput = null;
        try {
            File file = ResourceUtils.getFile("classpath:" + "help/" + commandLine + ".txt");
            helpInput = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            shellHelper.printWarning(e.getMessage());
            return;
        }
        try {
            String helpLine = helpInput.readLine();
            while (helpLine != null) {
                shellHelper.printInfo(helpLine);
                helpLine = helpInput.readLine();
            }
        } catch (java.io.IOException e) {
            shellHelper.printWarning("Failed to read help/help.txt: " + e.getLocalizedMessage());
        } finally {
            try {
                helpInput.close();
            } catch (IOException e) {

            }
        }
    }



    public Availability disconnectedCheck() {
        return !connected
                ? Availability.available()
                : Availability.unavailable("you are not connected");
    }

    @ShellMethod(value = "Connect to n2jss7 host, type \"connect\" without any parameters to display details")
    @ShellMethodAvailability("disconnectedCheck")
    public void connect(@ShellOption(arity = 2, defaultValue = "help") String[] args) {
        if (args.length != 2) {
            printHelp("connect");
            return;
        }
        try {
            nettyClient.setHost(args[0]);
            nettyClient.setPort(Integer.valueOf(args[1]));
            Thread clientThread = new Thread(nettyClient);
            clientThread.start();
            Thread.sleep(connectionTimeOut);
            nettyClient.future.channel().writeAndFlush(CONNECTING_GREETING);
            Thread.sleep(connectionTimeOut);
            Thread.sleep(connectionTimeOut);
        } catch (Exception e) {
            connected = false;
            shellHelper.printWarning("Error: " + e.getMessage());
            return;
        }


        if (clientHandler.getServerAnswer() == null || !CONNECTING_ESTABLISHED.equals(clientHandler.getServerAnswer())){
            connected = false;
            promptProvider.setShellPrefix(STANDARD_PROMPT);
            shellHelper.printWarning(clientHandler.getServerAnswer());
            clientHandler.setServerAnswer(null);
        } else {
            connected = true;
            promptProvider.setShellPrefix(String.format("n2jss7 connected(%s:%s) ==>", args[0],  args[1]));
        }

    }

    @ShellComponent
    @ShellCommandGroup("On connected commands")
    public class OnConnected {

        public Availability connectedCheck() {
            return connected
                    ? Availability.available()
                    : Availability.unavailable("you are not connected");
        }


        @ShellMethod("Disconnect from n2jss7 host, type \"disconnect help\" to display details")
        @ShellMethodAvailability("connectedCheck")
        public void disconnect(@ShellOption(defaultValue = "") String[] args) {
            if (args.length > 0) {
                printHelp("disconnect");
                return;
            }
            try {
                nettyClient.shutdown();
            } catch (Exception e) {
                shellHelper.printWarning("Error: " + e.getMessage());
                return;
            }
            connected = false;
            promptProvider.setShellPrefix(STANDARD_PROMPT);
            clientHandler.setServerAnswer(null);
        }

        @ShellMethod(value = "m3ua get \'parameter\'. Retrieve the current settings of the parameter.\n" +
                "Type \"m3ua get help\" to display list of parameters.\n" +
                "Type \"m3ua get \'parameter\' help\" to display detail of parameter.\n", key = "m3ua get")
        @ShellMethodAvailability("connectedCheck")
        public void m3ua_get(@ShellOption(defaultValue = "get") String[] args) throws InterruptedException {
            if (args.length != 1) {
                printHelp("m3ua_get");
                return;
            }
            nettyClient.future.channel().writeAndFlush("m3ua get " + args[args.length - 1]);
            Thread.sleep(connectionTimeOut);
            shellHelper.printInfo(clientHandler.getServerAnswer());
        }



        @ShellMethod(value = "m3ua get \'parameter\'. Retrieve the current settings of the parameter.\n" +
                "Type \"m3ua get help\" to display list of parameters.\n" +
                "Type \"m3ua get \'parameter\' help\" to display detail of parameter.\n", key = "m3ua asp")
        @ShellMethodAvailability("connectedCheck")
        public void m3ua_asp(@ShellOption(defaultValue = "show") String[] args) {
            if (args.length != 3) {
                printHelp("m3ua_get");
                return;
            }
            try {
                nettyClient.shutdown();
            } catch (Exception e) {
                shellHelper.printWarning("Error: " + e.getMessage());
                return;
            }
            connected = false;
            promptProvider.setShellPrefix(STANDARD_PROMPT);
            clientHandler.setServerAnswer(null);
        }



    }
}
