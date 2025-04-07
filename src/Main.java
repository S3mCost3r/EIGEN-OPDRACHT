import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class Person {
    private String naam;
    private String functie;
    private String locatie;
    private String posts;

    // Default constructor (nodig voor Jackson)
    public Person() {
    }

    public Person(String naam, String functie, String locatie, String posts) {
        this.naam = naam;
        this.functie = functie;
        this.locatie = locatie;
        this.posts = posts;
    }

    public String getNaam() {
        return naam;
    }

    public String getFunctie() {
        return functie;
    }

    public String getLocatie() {
        return locatie;
    }

    public String getPosts() {
        return posts;
    }
}
class Result {
    private Person person;
    private int score;

    public Result(Person person, int score) {
        this.person = person;
        this.score = score;
    }

    public Person getPerson() {
        return person;
    }

    public int getScore() {
        return score;
    }
}

interface KeywordMatcher {
    int match(Person person, String[] keywords);
}
class BasicKeywordMatcher implements KeywordMatcher {
    @Override
    public int match(Person person, String[] keywords) {
        int score = 0;
        for (String keyword : keywords) {
            score += countOccurrences(person.getNaam(), keyword);
            score += countOccurrences(person.getFunctie(), keyword);
            score += countOccurrences(person.getLocatie(), keyword);
            score += countOccurrences(person.getPosts(), keyword);
        }
        return score;
    }

    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) return 0;
        int count = 0, index = 0;
        text = text.toLowerCase();
        keyword = keyword.toLowerCase();
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}


class DataLoader {
    public List<Person> load(String jsonFileName) {
        try {
            String jsonContent = Files.readString(Paths.get(jsonFileName));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonContent, new TypeReference<List<Person>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}


class SearchService {
    private final KeywordMatcher matcher;
    private final DataLoader loader;

    public SearchService(KeywordMatcher matcher, DataLoader loader) {
        this.matcher = matcher;
        this.loader = loader;
    }

    public List<Result> search(String fileName, String[] keywords) {
        List<Person> persons = loader.load(fileName);
        List<Result> results = new ArrayList<>();

        for (Person person : persons) {
            int score = matcher.match(person, keywords);
            if (score > 0) {
                results.add(new Result(person, score));
            }
        }

        // Sorteer de resultaten: hoogste score eerst
        results.sort(Comparator.comparing(Result::getScore).reversed());
        return results;
    }
}


public class Main {
    public static void main(String[] args) {
        // Vraag de gebruiker om 1 of 2 keywords (gescheiden door spaties)
        Scanner scanner = new Scanner(System.in);
        System.out.println("Voer 1 of 2 keywords in (gescheiden door spaties): ");
        String[] keywords = scanner.nextLine().trim().split("\\s+");

        // Maak de benodigde objecten aan
        KeywordMatcher matcher = new BasicKeywordMatcher();
        DataLoader loader = new DataLoader();
        SearchService service = new SearchService(matcher, loader);

        // Voer de zoekopdracht uit op het JSON-bestand "Data.json"
        List<Result> results = service.search("Data.json", keywords);

        // Controleer of er resultaten zijn en print ze
        if (results.isEmpty()) {
            System.out.println("Geen resultaten gevonden.");
        } else {
            System.out.println("\nGeschikte business partners:");
            for (Result r : results) {
                System.out.println(r.getPerson().getNaam() + " - Score: " + r.getScore());
            }
        }
    }
}





