package test.raddress.prove;

import it.myideas.raddress.model.Address;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Test;

public class SimpleMain {

	@Test
	public void aha() throws IOException {
		System.out.println(Normalizer.normalize("àèéìòù", Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", ""));
		
		Date date = new Date(144238);
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
		String dateFormatted = formatter.format(date);
		
		long millis = 144238;
		long second = (millis / 1000) % 60;
		long minute = (millis / (1000 * 60)) % 60;
		long hour = (millis / (1000 * 60 * 60)) % 24;

		String time = String.format("%02dh %02dm %02ds %dms", hour, minute, second, millis);
		
		System.out.println(time);
		
//		System.out.println("mmmm");
//		String a = "  via le mani dal naso  ";
//		String[] c = a.split("");
//		ArrayList<String> tokens = new ArrayList<String>();
//		
//		for(int i=0; i<c.length-2; i++) {
//			System.out.println(c[i]);
//			tokens.add(c[i] + c[i+1] + c[i+2]);
//		}
//		
//		tokens.forEach(s -> {
//			System.out.println("["+s+"]");
//		});
//		System.out.println("fatto");
//		
//	
		Path path = Paths.get(".", "address.csv");
		System.out.println("Original: " + Files.lines(path).skip(1).count());
		
		long distinct = Files.lines(path).skip(1).map(s->{
			String[] parts = s.split(",");
			return new Address(parts[0], parts[1], parts[2]);
		}).filter(distinctByKey(a -> a.getAddress())).count();
		System.out.println("Distinct: " +distinct);
		
		
		Files.lines(path).skip(1).map(s->{
			String[] parts = s.split(",");
			return new Address(parts[0], parts[1], parts[2]);
		}).filter(distinctByKey(a -> a.getAddress()))
		.forEach(s -> System.out.println(s));
		
		System.out.println((int)(Math.random() * 100));
		
		
//		try(Stream<String> lines = Files.lines(path).skip(1)) {
//			lines.forEach(s-> {
//				System.out.println(s);
//			});
//		}
	}

	
	public static <T> Predicate<T> distinctByKey(Function<? super T,Object> keyExtractor) {
	    Map<Object,Boolean> seen = new ConcurrentHashMap<>();
	    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}
}


