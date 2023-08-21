package ru.mediatel.shellclient.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;
import ru.mediatel.shellclient.client.ClientHandler;
import ru.mediatel.shellclient.client.NettyClient;
import ru.mediatel.shellclient.config.CustomPromptProvider;
import ru.mediatel.shellclient.config.ShellHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private final Set<String> m3uaAsParameters = new HashSet<>(Arrays.asList(
            "show",
            "create",
            "destroy",
            "add",
            "remove"
    ));

    private final Set<String> m3uaRouteParameters = new HashSet<>(Arrays.asList(
            "show",
            "add",
            "remove"
    ));

    private final Set<String> sccpSetGetParameters = new HashSet<>(Arrays.asList(
            "sccpprotocolversion",
            "periodoflogging",
            "maxdatamessage",
            "previewmode",
            "reassemblytimerdelay",
            "removespc",
            "respectpc",
            "ssttimerduration_increasefactor",
            "ssttimerduration_max",
            "ssttimerduration_min",
            "zmarginxudtmessage",
            "cc_timer_a",
            "cc_timer_d",
            "canrelay",
            "connesttimerdelay",
            "iastimerdelay",
            "iartimerdelay",
            "reltimerdelay",
            "repeatreltimerdelay",
            "inttimerdelay",
            "guardtimerdelay",
            "resettimerdelay",
            "timerexecutors_threadcount",
            "cc_algo",
            "cc_blockingoutgoungsccpmessages"
    ));

    private final Set<String> sccpExtensionParameters = new HashSet<>(Arrays.asList(
            "show",
            "create",
            "modify",
            "delete"
    ));

    private final Set<String> sctpSetGetParameters = new HashSet<>(Arrays.asList(
            "connectdelay",
            "cc_delaythreshold_1",
            "cc_delaythreshold_2",
            "cc_delaythreshold_3",
            "cc_backtonormal_delaythreshold_1",
            "cc_backtonormal_delaythreshold_2",
            "cc_backtonormal_delaythreshold_3"
    ));

    private final Set<String> sctpServerParameters = new HashSet<>(Arrays.asList(
            "show",
            "modify",
            "create",
            "destroy",
            "start",
            "stop"
    ));

    private final Set<String> sctpAssociationParameters = new HashSet<>(Arrays.asList(
            "show",
            "modify",
            "create",
            "destroy"
    ));

    private final Set<String> tcapSetGetParameters = new HashSet<>(Arrays.asList(
            "dialogidletimeout",
            "dialogidrangeend",
            "dialogidrangestart",
            "donotsendprotocolversion",
            "invoketimeout",
            "maxdialogs",
            "previewmode",
            "statisticsenabled",
            "swaptcapidbytes",
            "executordelaythreshold_1",
            "executordelaythreshold_2",
            "executordelaythreshold_3",
            "executorbacktonormaldelaythreshold_1",
            "executorbacktonormaldelaythreshold_2",
            "executorbacktonormaldelaythreshold_3",
            "memorythreshold_1",
            "memorythreshold_2",
            "memorythreshold_3",
            "backtonormalmemorythreshold_1",
            "backtonormalmemorythreshold_2",
            "backtonormalmemorythreshold_3",
            "blockingincomingtcapmessages",
            "slsrange"
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
            InputStream resource = new ClassPathResource("help/" + commandLine + ".txt").getInputStream();
            helpInput = new BufferedReader(new InputStreamReader(resource));
        } catch (Exception e) {
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

        @ShellMethod(value = "m3ua set 'parameter'.\n" +
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
        @ShellMethod(value = "m3ua as 'parameter'.\n" +
                "Type \"m3ua as help\" to display list of parameters.\n" +
                "Type \"m3ua as 'parameter' help\" to display detail of parameter.\n", key = "m3ua as")
        @ShellMethodAvailability("connectedCheck")
        public void m3ua_as(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && m3uaAsParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("m3ua_as_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("m3ua as " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("m3ua_as");
            }
        }
        @ShellMethod(value = "m3ua route 'parameter'.\n" +
                "Type \"m3ua route help\" to display list of parameters.\n" +
                "Type \"m3ua route 'parameter' help\" to display detail of parameter.\n", key = "m3ua route")
        @ShellMethodAvailability("connectedCheck")
        public void m3ua_route(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && m3uaRouteParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("m3ua_route_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("m3ua route " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("m3ua_route");
            }
        }
        @ShellMethod(value = "sccp get 'parameter'.\n" +
                "Type \"sccp get help\" to display list of parameters.\n" +
                "Type \"sccp get 'parameter' help\" to display detail of parameter.\n", key = "sccp get")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_get(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpSetGetParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_set_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp get " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_get");
            }
        }

        @ShellMethod(value = "sccp set 'parameter'.\n" +
                "Type \"sccp set help\" to display list of parameters.\n" +
                "Type \"sccp set 'parameter' help\" to display detail of parameter.\n", key = "sccp set")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_set(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpSetGetParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_set_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp set " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_set");
            }
        }

        @ShellMethod(value = "sccp sap 'parameter'. Handle Service Access Points.\n" +
                "Type \"sccp sap help\" to display list of parameters.\n" +
                "Type \"sccp sap 'parameter' help\" to display detail of parameter.\n", key = "sccp sap")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_sap(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_sap_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp sap " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_sap");
            }
        }

        @ShellMethod(value = "sccp dest 'parameter'. Handle details of all destinations specified for a Service Access Point.\n" +
                "Type \"sccp dest help\" to display list of parameters.\n" +
                "Type \"sccp dest 'parameter' help\" to display detail of parameter.\n", key = "sccp dest")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_dest(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_dest_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp dest " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_dest");
            }
        }

        @ShellMethod(value = "sccp address 'parameter'. Handle SCCP Addresses.\n" +
                "Type \"sccp address help\" to display list of parameters.\n" +
                "Type \"sccp address 'parameter' help\" to display detail of parameter.\n", key = "sccp address")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_address(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_address_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp address " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_address");
            }
        }

        @ShellMethod(value = "sccp rule 'parameter'. Handle SCCP Rules.\n" +
                "Type \"sccp rule help\" to display list of parameters.\n" +
                "Type \"sccp rule 'parameter' help\" to display detail of parameter.\n", key = "sccp rule")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_rule(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_rule_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp rule " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_rule");
            }
        }

        @ShellMethod(value = "sccp rsp 'parameter'. Handle Remote Signaling Points.\n" +
                "Type \"sccp rsp help\" to display list of parameters.\n" +
                "Type \"sccp rsp 'parameter' help\" to display detail of parameter.\n", key = "sccp rsp")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_rsp(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_rsp_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp rsp " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_rsp");
            }
        }

        @ShellMethod(value = "sccp rss 'parameter'. Handle Remote Sub-Systems.\n" +
                "Type \"sccp rss help\" to display list of parameters.\n" +
                "Type \"sccp rss 'parameter' help\" to display detail of parameter.\n", key = "sccp rss")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_rss(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_rss_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp rss " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_rss");
            }
        }

        @ShellMethod(value = "sccp lmr 'parameter'. Handle Long Message Rules.\n" +
                "Type \"sccp lmr help\" to display list of parameters.\n" +
                "Type \"sccp lmr 'parameter' help\" to display detail of parameter.\n", key = "sccp lmr")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_lmr(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_lmr_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp lmr " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_lmr");
            }
        }

        @ShellMethod(value = "sccp csp 'parameter'. Handle Concerned Signaling Point Codes.\n" +
                "Type \"sccp csp help\" to display list of parameters.\n" +
                "Type \"sccp csp 'parameter' help\" to display detail of parameter.\n", key = "sccp csp")
        @ShellMethodAvailability("connectedCheck")
        public void sccp_csp(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sccpExtensionParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sccp_csp_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sccp csp " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sccp_csp");
            }
        }

        @ShellMethod(value = "sctp set 'parameter'. Manage SCTP stack properties.\n" +
                "Type \"sctp set help\" to display list of parameters.\n" +
                "Type \"sctp set 'parameter' help\" to display detail of parameter.\n", key = "sctp set")
        @ShellMethodAvailability("connectedCheck")
        public void sctp_set(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sctpSetGetParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sctp_set_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sctp set " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sctp_set");
            }
        }

        @ShellMethod(value = "sctp get 'parameter'. Manage SCTP stack properties.\n" +
                "Type \"sctp get help\" to display list of parameters.\n" +
                "Type \"sctp get 'parameter' help\" to display detail of parameter.\n", key = "sctp get")
        @ShellMethodAvailability("connectedCheck")
        public void sctp_get(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sctpSetGetParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sctp_get_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sctp get " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sctp_set");
            }
        }

        @ShellMethod(value = "sctp server 'parameter'. Manage SCTP server instance.\n" +
                "Type \"sctp server help\" to display list of parameters.\n" +
                "Type \"sctp server 'parameter' help\" to display detail of parameter.\n", key = "sctp server")
        @ShellMethodAvailability("connectedCheck")
        public void sctp_server(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sctpServerParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sctp_server_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sctp server " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sctp_server");
            }
        }

        @ShellMethod(value = "sctp association 'parameter'. Manage SCTP Association.\n" +
                "Type \"sctp association help\" to display list of parameters.\n" +
                "Type \"sctp association 'parameter' help\" to display detail of parameter.\n", key = "sctp association")
        @ShellMethodAvailability("connectedCheck")
        public void sctp_association(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0 && sctpAssociationParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("sctp_association_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("sctp association " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("sctp_association");
            }
        }

        @ShellMethod(value = "tcap set 'parameter'. Manage TCAP configurations.\n" +
                "Type \"tcap set help\" to display list of parameters.\n" +
                "Type \"tcap set 'parameter' help\" to display detail of parameter.\n", key = "tcap set")
        @ShellMethodAvailability("connectedCheck")
        public void tcap_set(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0
                    && tcapSetGetParameters.contains(args[0])
                    && !"previewmode".equals(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("tcap_set_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("tcap set " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("tcap_set");
            }
        }


        @ShellMethod(value = "tcap get 'parameter'. Manage TCAP configurations.\n" +
                "Type \"tcap get help\" to display list of parameters.\n" +
                "Type \"tcap get 'parameter' help\" to display detail of parameter.\n", key = "tcap get")
        @ShellMethodAvailability("connectedCheck")
        public void tcap_get(@ShellOption(arity = 10) String[] args) {
            if (args.length > 0
                    && tcapSetGetParameters.contains(args[0])) {
                String command = String.join(" ", args);
                if (command.contains("help")) {
                    printHelp("tcap_get_" + args[0]);
                    return;
                }
                try {
                    nettyClient.future.channel().writeAndFlush("tcap get " + command);
                    Thread.sleep(connectionTimeOut);
                    shellHelper.printInfo(clientHandler.getServerAnswer());
                } catch (InterruptedException e) {
                    shellHelper.printWarning("Error: " + e.getMessage());
                }
            } else {
                printHelp("tcap_et");
            }
        }


    }

}
