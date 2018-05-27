package com.rhfung.p2pd;
import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.P2PDictionary.peers.GenericBonjour;
import com.rhfung.P2PDictionary.peers.HelloDiscovery;
import com.rhfung.P2PDictionary.peers.NoDiscovery;
import com.rhfung.P2PDictionary.peers.WindowsBonjour;
import com.rhfung.logging.LogInstructions;
import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by richard on 1/18/16.
 */
public class Server {

    private static final Logger logger = Logger.getGlobal();

    private static Level logLevel = Level.ALL;
    static {
        // Remove all the default handlers (usually just one console handler)
        Logger rootLogger = Logger.getLogger("");
        Handler[] rootHandlers = rootLogger.getHandlers();
        for (Handler handler : rootHandlers) {
            rootLogger.removeHandler(handler);
        }

        // Add our own handler
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(logLevel);
        logger.addHandler(handler);
        logger.setLevel(logLevel);
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("m", "description", true, "Description for the server");
        options.addOption("p", "port", true, "Bind to port default:8765");
        options.addOption("s", "namespace", true, "Namespace for the server");
        options.addOption("t", "timespan", true, "Search interval for clients in milliseconds");
        options.addOption("d", "discovery", true, "Backend discovery mechanism: none, bonjour, win-bonjour, hello. Default: hello");
        options.addOption("h", "help", true, "Show this help");
        options.addOption(Option.builder()
                .longOpt("pattern")
                .hasArg()
                .desc("Monitors a specific pattern using wildcard (*), single character (?), and number (#) placeholders; default to *")
                .build());
        options.addOption(Option.builder()
                .longOpt("nopattern")
                .desc("Monitors no patterns")
                .build());
        options.addOption(Option.builder()
                .longOpt("debug")
                .desc("Enable debugging mode")
                .build());
        options.addOption(Option.builder()
                .longOpt("fulldebug")
                .desc("Enable debugging mode")
                .build());
        options.addOption(Option.builder("n")
                .longOpt("node")
                .argName("host:port")
                .desc("Provide clients in the form hostname:port,hostname:port,... (separated by commas)")
                .hasArgs()
                .valueSeparator(',')
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("p2pd", options);
            return;
        }

        P2PDictionary.Builder builder = P2PDictionary.builder();

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("p2pd", options);
            return;
        }

        if (cmd.hasOption("description")) {
            builder.setDescription(cmd.getOptionValue("description"));
        }

        if (cmd.hasOption("port")) {
            builder.setPort(Integer.parseInt(cmd.getOptionValue("port")));
        }

        if (cmd.hasOption("namespace")) {
            builder.setNamespace(cmd.getOptionValue("namespace"));
        }

        if (cmd.hasOption("timespan")) {
            builder.setClientSearchTimespan(Integer.parseInt(cmd.getOptionValue("timespan")));
        }

        if (cmd.hasOption("discovery")) {
            if (cmd.getOptionValue("discovery").equals("none")) {
                builder.setPeerDiscovery(new NoDiscovery());
            } else if (cmd.getOptionValue("discovery").equals("win-bonjour")) {
                builder.setPeerDiscovery(new WindowsBonjour());
            } else if (cmd.getOptionValue("discovery").equals("bonjour")) {
                builder.setPeerDiscovery(new GenericBonjour());
            } else if (cmd.getOptionValue("discovery").equals("hello")) {
                builder.setPeerDiscovery(new HelloDiscovery());
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("p2pd", options);
                return;
            }
        } else {
            builder.setPeerDiscovery(new HelloDiscovery());
        }

        if (cmd.hasOption("clients")) {
            String[] clientList = cmd.getOptionValues("clients");
            for (String client: clientList) {
                if (!client.contains(":")) {
                    System.out.println("Invalid host:port pair " + client);
                    return;
                }
            }
        }

        if (cmd.hasOption("fulldebug")) {
            builder.setLogLevel(System.out, LogInstructions.DEBUG);
        }
        else if (cmd.hasOption("debug")) {
            builder.setLogLevel(System.out, LogInstructions.INFO);
        }

        final P2PDictionary dict = builder.build();

        if (cmd.hasOption("nopattern")) {
            // do nothing
            if (cmd.hasOption("pattern")) {
                System.out.println("Ignoring pattern match for " + cmd.getOptionValue("pattern"));
            }
        }
        else if (cmd.hasOption("pattern")) {
            dict.addSubscription(cmd.getOptionValue("pattern"));
        } else {
            dict.addSubscription("*");
        }

        Thread shutdown = new Thread(new Runnable() {
            @Override
            public void run() {
            System.out.println("Closing server");
            dict.close();
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdown);

        System.out.println("Started server");

        if (cmd.hasOption("node")) {
            String[] clientList = cmd.getOptionValues("node");
            for (String client: clientList) {
                String parts[] = client.split(":", 2);
                if (parts.length == 2) {
                    try {
                        InetAddress address = InetAddress.getByName(parts[0]);
                        int port = Integer.parseInt(parts[1]);
                        System.out.println("Connecting to " + client);
                        dict.openClient(address, port);
                    } catch (UnknownHostException exception) {
                        System.out.println("Cannot connect to " + client);
                    }
                } else {
                    System.out.println("Specify port to connect to " + client);
                }
            }
        }

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("Waiting for server to close");
        shutdown.start();
        try {
            shutdown.join(5000);
        } catch (InterruptedException ex) {
            // pass
        }
    }
}
