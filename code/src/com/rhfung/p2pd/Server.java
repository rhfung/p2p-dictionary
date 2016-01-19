package com.rhfung.p2pd;
import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.P2PDictionary.peers.NoDiscovery;
import com.rhfung.P2PDictionary.peers.WindowsBonjour;
import org.apache.commons.cli.*;

import java.io.IOException;

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
        options.addOption("debug", false, "Enable debugging mode");
        options.addOption("fulldebug", false, "Enable debugging mode");

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

        final P2PDictionary dict = builder.build();

        if (cmd.hasOption("fulldebug")) {
            dict.setDebugBuffer(System.out, 0, true);
        }
        else if (cmd.hasOption("debug")) {
            dict.setDebugBuffer(System.out, 1, true);
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
