package it.myideas.raddress.indexer;

import it.myideas.raddress.Main;
import it.myideas.raddress.model.Address;
import it.myideas.raddress.model.AddressResult;
import it.myideas.raddress.strings.Levenshtein;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

/**
 * A Redis-index suitable for address matching
 *
 * @author Tommaso Doninelli
 *
 */
public class Indexer {

    private static Logger log = LoggerFactory.getLogger(Indexer.class);

    private static final String KEY_ADDR = "addr:";
    private static final String KEY_TOK = "tok:";

    private JedisPool jedisPool;
    private int nResults = 4;

    public Indexer(CommandLine options) {

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(100);
        String host = options.getOptionValue(Main.OPT_K_HOST, "localhost");

        log.info("Connecting to " + host);

        this.jedisPool = new JedisPool(new JedisPoolConfig(), host);
        this.nResults = Integer.parseInt(options.getOptionValue(Main.OPT_K_NRES, "4"));
    }

    /**
     *
     * @param input
     * @return a {@link List<Address>} of match
     */
    public List<AddressResult> getCandidates(String input) {

        /*
         * Simple algorithm:
         * find all the address that belongs in the intersection of all the token-sets.
         * Then do a Levensthein distance in the set. 
         */
        
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipe = jedis.pipelined();

            // Temporary result will be stored in this key.
            String tempKey = "temp:search:" + (int) (Math.random() * 100);
//			log.debug("The temporary result will be in {}", tempKey);

            // Get the tok for input
            List<String> keys = getToks(input).stream()
                    .map(s -> {return KEY_TOK + s;})
                    .collect(Collectors.toList());

            // Do the magic
            pipe.zunionstore(tempKey, keys.toArray(new String[]{}));

            // Get the first 4 results
            Response<Set<Tuple>> candidates = pipe.zrevrangeByScoreWithScores(tempKey, Double.MAX_VALUE, 1d, 0, nResults);

            // Collect all the requests
            Comparator<Address> c = (a, b) ->{
                return Levenshtein.compareAgainst.apply(input).compare(a.getFQAddress(), b.getFQAddress());
            };
            
            // Get the address for each candidate
            pipe.sync();
            
            List<AddressResult> res = candidates.get().stream()
                .map(candidate -> {
                    Map<String, String> data = jedisPool.getResource().hgetAll(KEY_ADDR + candidate.getElement());
                    
                    return new AddressResult(
                        candidate.getElement(), // IDNO 
                        data.get("city"),
                        data.get("address"),
                        candidate.getScore());
                })
                .sorted(c)
                .collect(Collectors.toList());

            
            // Clean up! Remove temporary key
            jedis.del(tempKey);
            
            return res;
        }

    }

    /**
     * Add an {@link Address} to the index.
     *
     * @param address
     */
    public void add(Address address) {
        /*
        * How is built an index?
        * 1) Address index
        *    *hash* addr:<id> -> <full address>
        * 2) tok-index
        * 	  *sprted set* tok:<tok> -> <id>, <id>, <id>
        * 	The score is the "number of times the address has this tok" 
        * 
        * In italiano perchè sono le 3:
        * Un token è una sequenza di 3 char. 
        * Una stringa con un indirizzo è tokenizzata ("  v", " vi", "via", "ia ", "a l" )
        * In Redis ho una key per ogni token, dove il valore è un sorted set. 
        * 	L'i-esimo elemento è una addr:<idno>, il cui score è quante volte il tok è presente nella stringa
        * 
        *  Quando faccio un retrieval faccio un UNION-SET ordinato sommando gli score degli elementi
        *  che compaiono in tutti i set token. 
        */
        try (Jedis jedis = jedisPool.getResource()) {

            Pipeline pipe = jedis.pipelined();

            // Index the address
            pipe.hmset(KEY_ADDR + address.getIdno(), new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;

                {
                    put("city", address.getCity());
                    put("address", address.getAddress());
                }
            });

            // Building the toks!		
            // tok:<tok> -> {<address id>}
            String idno = address.getIdno(); // Avra' effetto?
            getToks(address.getFQAddress()).forEach(tok -> {
                pipe.zincrby(KEY_TOK + tok, 1, idno);
            });

            pipe.sync();
            log.debug("tok done for {}", idno);

        }

    }

    /**
     * Returns a list of tokens. A token is 3-letter The input string is cleaned
     * and normalize (no strange chars, lowercase)
     *
     * @param str
     * @return
     */
    private ArrayList<String> getToks(String str) {

        // Cleanup the string: lower case
        String addr = str.toLowerCase();

        // Strip accented chars (probably not needed)
        addr = Normalizer.normalize(addr, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Redis handles the white spaces, so add 2 blanks to have 3-chars tok easily
        addr = "  " + addr + "  ";

        String[] c = addr.split("");
        ArrayList<String> tokens = new ArrayList<String>();
        for (int i = 0; i < c.length - 2; i++) {
            tokens.add(c[i] + c[i + 1] + c[i + 2]);
        }

        return tokens;
    }

    private class TempResult {

        private String idno;
        private double score;
        private Response<Map<String, String>> response;

        public TempResult(String idno, double score, Response<Map<String, String>> response) {
            this.idno = idno;
            this.score = score;
            this.response = response;
        }

        @Override
        public String toString() {
            return String.format("[%s] [%s] {%s, %s}", idno, score, response.get().get("address"), response.get().get("city"));
        }
        
        
    }
}
