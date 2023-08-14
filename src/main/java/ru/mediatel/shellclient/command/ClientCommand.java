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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ShellComponent
@ConfigurationProperties(prefix="jss7-shell")
public class ClientCommand {
    private final String STANDARD_PROMPT = "n2jss7 client ==> ";
    private final String CONNECTING_GREETING = "Try connect";
    private final String CONNECTING_ESTABLISHED = "Connection established successfully";
    private final NettyClient nettyClient;
    private final CustomPromptProvider promptProvider;
    private final ClientHandler clientHandler;
    private final ShellHelper shellHelper;
    private final Set<String> m3uaGetParameters = new HashSet<>(Arrays.asList(
            "maxsequencenumber",
            "deliverymessagethreadcount",
            "maxasforroute",
            "routinglabelformat"
    ));
    private final Set<String> m3uaAspParameters = new HashSet<>(Arrays.asList(
            "show",
            "create",
            "destroy",
            "stop",
            "start"
    ));
    private final Set<String> m3uaSetParameters = new HashSet<>(Arrays.asList(
            "heartbeattime",
            "statisticsenabled",
            "routingkeymanagementenabled",
            "uselsbforlinksetselection"
    ));

    @Value("${connectionTimeOut}")
    private int connectionTimeOut;


    public ClientCommand(NettyClient nettyClient, CustomPromptProvider promptProvider, ClientHandler clientHandler, ShellHelper shellHelper) {
        this.nettyClient = nettyClient;
        this.promptProvider = promptProvider;
        this.clientHandler = clientHandler;
        this.shellHelper = shellHelper;
    }

    protected void printHelp(String commandLine) {
        BufferedReader helpInput;
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
            shellHelper.printWarning("Failed to read help/" + commandLine + ": " + e.getLocalizedMessage());
        } finally {
            try {
                helpInput.close();
            } catch (IOException e) {
                shellHelper.printInfo("Failed to close help/" + commandLine + ": " + e.getLocalizedMessage());
            }
        }
    }



    public Availability disconnectedCheck() {
        return !nettyClient.isConnected()
                ? Availability.available()
                : Availability.unavailable("You are already connected");
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
            nettyClient.setPort(Integer.parseInt(args[1]));
            Thread clientThread = new Thread(nettyClient);
            clientThread.start();
            Thread.sleep(connectionTimeOut);
            nettyClient.future.channel().writeAndFlush(CONNECTING_GREETING);
            Thread.sleep(connectionTimeOut);
            if (CONNECTING_ESTABLISHED.equals(clientHandler.getServerAnswer())) {
                shellHelper.printInfo(clientHandler.getServerAnswer());
            }
            Thread.sleep(connectionTimeOut);
        } catch (Exception e) {
            shellHelper.printWarning("Error: " + e.getMessage());
            return;
        }


        if (clientHandler.getServerAnswer() == null || !CONNECTING_ESTABLISHED.equals(clientHandler.getServerAnswer())){
            promptProvider.setShellPrefix(STANDARD_PROMPT);
            shellHelper.printWarning(clientHandler.getServerAnswer());
            clientHandler.setServerAnswer(null);
        } else {
            promptProvider.setShellPrefix(String.format("n2jss7 connected(%s:%s) ==>", args[0],  args[1]));
        }

    }

    @ShellComponent
    @ShellCommandGroup("On connected commands")
    public class OnConnected {

        public Availability connectedCheck() {
            return nettyClient.isConnected()
                    ? Availability.available()
                    : Availability.unavailable("You are not connected");
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
            promptProvider.setShellPrefix(STANDARD_PROMPT);
            clientHandler.setServerAnswer(null);
        }

        @ShellMethod(value = "m3ua get 'parameter'. Retrieve the current settings of the parameter.\n" +
                "Type \"m3ua get help\" to display list of parameters.\n" +
                "Type \"m3ua get 'parameter' help\" to display detail of parameter.\n", key = "m3ua get")
        @ShellMethodAvailability("connectedCheck")
        public void m3ua_get(@ShellOption(arity = 10) String[] args) {
            if (args.length == 1 && m3uaGetParameters.contains(args[0])) {
                try {
                    nettyClient.future.channel().writeAndFlush("m3ua get " + args[0]);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else if (args.length == 2 && m3uaGetParameters.contains(args[0]) && "help".equals(args[1])) {
                printHelp("m3ua_get_" + args[0]);
            } else {
                printHelp("m3ua_get");
            }
        }



        @ShellMethod(value = "m3ua asp 'parameter'. Handle asp state.\n" +
                "Type \"m3ua asp help\" to display list of parameters.\n" +
                "Type \"m3ua asp 'parameter' help\" to display detail of parameter.\n", key = "m3ua asp")
        @ShellMethodAvailability("connectedCheck")
        public void m3ua_asp(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && m3uaAspParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("m3ua_asp_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("m3ua asp " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("m3ua_asp");
            }
        }

        @ShellMethod(value = "m3ua set 'parameter'. Set m3ua parameters.\n" +
                "Type \"m3ua set help\" to display list of parameters.\n" +
                "Type \"m3ua set 'parameter' help\" to display detail of parameter.\n", key = "m3ua set")
        @ShellMethodAvailability("connectedCheck")
        public void m3ua_set(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && m3uaSetParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("m3ua_set_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("m3ua set " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("m3ua_set");
            }
        }
    }

}