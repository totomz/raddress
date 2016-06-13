package it.myideas.raddress;

import it.myideas.raddress.indexer.Indexer;
import it.myideas.raddress.model.Address;
import it.myideas.raddress.model.AddressResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info");
        System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(org.slf4j.impl.SimpleLogger.DATE_TIME_FORMAT_KEY, "HH:mm:ss.SSS");
    }

    public static final String OPT_K_INDEX = "index";
    public static final String OPT_K_NRES = "nres";
    public static final String OPT_K_HOST = "host";
    public static final String OPT_K_LOG = "log";
    private static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ParseException {

        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption(OPT_K_INDEX, false, "Build the index (WILL DROP THE DATABASE!) - a file \"address.csv\" is expected");
        options.addOption(OPT_K_NRES, true, "Number of results to be returned");
        options.addOption(OPT_K_HOST, true, "IP address for the Redis instance [localhost]");
        options.addOption(OPT_K_LOG, true, "Log level [info]");

//        args = new String[]{"vai alba"};
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdl = parser.parse(options, args);
        

        if (args.length == 0) {            
            HelpFormatter help = new HelpFormatter();
            help.printHelp("raddress", options);
            System.exit(0);
        }

        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info");

        if (cmdl.hasOption("index")) {
            String file = "address.csv";
            buildIndexFromCSV(file, cmdl);
        } else {

            Indexer indexer = new Indexer(cmdl);
            String request = cmdl.getArgList().stream().collect(Collectors.joining(" "));

            if (request == null || request.isEmpty()) {
                log.error("No address specified");
                System.exit(1);
            }

            log.info(String.format("Looking for '%s'", request));

            long start = System.currentTimeMillis();
            List<AddressResult> results = indexer.getCandidates(request);
            String time = msToTime(System.currentTimeMillis() - start);

            results.forEach(r -> log.info(r.toString()));

            log.info("Search completed in " + time);

        }
    }

    private static void buildIndexFromCSV(String file, CommandLine cmdl) throws IOException {
        log.info("Loading " + file);

        // Get the stream and skip the header
        // The parallel() stream sucks. Good old ThreadPool!
        ExecutorService pool = Executors.newFixedThreadPool(90);

        Indexer indexer = new Indexer(cmdl);

        long start = System.currentTimeMillis();

        Files.lines(Paths.get(".", file))
                .skip(1)
                .map(s -> {
                    String[] parts = s.split(",");
                    return new Address(parts[0], parts[1], parts[2]);
                })
                .filter(distinctByKey(a -> a.getFQAddress())) // Remove duplicates! :) FIXME What identify an address as "duplicated"?
                .forEach(address -> {
                    pool.submit(() -> {
                        indexer.add(address);
                    });
                });

        pool.shutdown();
        log.info("....waiting....");

        try {
            pool.awaitTermination(5, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        String time = msToTime(System.currentTimeMillis() - start);
        log.info("Index done in " + time);
    }

    private static String msToTime(long millis) {
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d:%d", hour, minute, second, millis);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

}
