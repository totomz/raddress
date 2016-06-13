package it.myideas.raddress.strings;

import java.util.Comparator;
import javaslang.Function1;
import javaslang.Function3;

/**
 *
 * @author https://rosettacode.org/wiki/Levenshtein_distance#Java
 */
public class Levenshtein {

    public static int distance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
    
    Comparator<String> s = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    /**
     * A comparable that compare 2 strings against a given target
     */
    public static Function1<String, Comparator<String>> compareAgainst = (target) -> {
        return (a, b)-> {
            int da = Levenshtein.distance(target, a);
            int db = Levenshtein.distance(target, b);
            return Integer.compare(da, db);
        };
    };
    
}
