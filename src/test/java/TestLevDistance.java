
import it.myideas.raddress.strings.Levenshtein;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author tommaso
 */
public class TestLevDistance {

    
    @Test
    public void testLevDistance() {
        
        String target = "kitting";
        
        Assert.assertEquals(2, Levenshtein.distance(target, "kitten"));
        Assert.assertEquals(1, Levenshtein.distance(target, "sitting"));
        Assert.assertEquals(11, Levenshtein.distance(target, "raisethysword"));        
    }
    
    @Test
    public void compareByDistance() {
        String target = "kitting";
        List<String> sorted = Stream.of("primo", "secondo", "sitting", "kitten", "ciaone")
                .sorted(Levenshtein.compareAgainst.apply(target))
                .collect(Collectors.toList());
        
        Assert.assertEquals("sitting", sorted.get(0));
        Assert.assertEquals("kitten", sorted.get(1));
        Assert.assertEquals("ciaone", sorted.get(2));
        Assert.assertEquals("primo", sorted.get(3));
        Assert.assertEquals("secondo", sorted.get(4));
        
    }
            
}
