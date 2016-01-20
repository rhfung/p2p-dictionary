package com.rhfung.p2pd;
import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.P2PDictionary.peers.NoDiscovery;
import com.rhfung.P2PDictionary.peers.WindowsBonjour;
import com.rhfung.logging.LogInstructions;
import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by richard on 1/18/16.
 */
public class Server {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("m", "description", true, "Description for the server");
        options.addOption("p", true, "Port that the server binds to");
        options.addOption("ns", "namespace", true, "Namespace for the server");
        options.addOption("t", "timespan", true, "Search timespan for clients");
        options.addOption("d", "discovery", true, "Backend discovery mechanism: none, win-bonjour");
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
        options.addOption("debug", false, "Enable debugging mode");
        options.addOption("fulldebug", false, "Enable debugging mode");
        options.addOption(Option.builder("c")
                .longOpt("clients")
                .argName("hosts")
                .desc("Provide clients in the form hostname:port,hostname:port,... (separated by comma)")
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

        P2PDictionary.Builder builder = new P2PDictionary.Builder();

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("p2pd", options);
            return;
        }

        if (cmd.hasOption("description")) {
            builder.setDescription(cmd.getOptionValue("description"));
        }

        if (cmd.hasOption("p")) {
            builder.setPort(Integer.parseInt(cmd.getOptionValue("p")));
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
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("p2pd", options);
                return;
            }
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

        final P2PDictionary dict = builder.build();

        if (cmd.hasOption("fulldebug")) {
            dict.setDebugBuffer(System.out, LogInstructions.DEBUG, true);
        }
        else if (cmd.hasOption("debug")) {
            dict.setDebugBuffer(System.out, LogInstructions.INFO, true);
        }

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

        if (cmd.hasOption("clients")) {
            String[] clientList = cmd.getOptionValues("clients");
            for (String client: clientList) {
                String parts[] = client.split(":", 2);
                try {
                    InetAddress address = InetAddress.getByName(parts[0]);
                    int port = Integer.parseInt(parts[1]);
                    System.out.println("Connecting to " + client);
                    dict.openClient(address, port);
                } catch (UnknownHostException exception) {
                    System.out.println("Cannot connect to " + client);
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
