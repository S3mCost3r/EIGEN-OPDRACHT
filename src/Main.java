import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main extends Application {

    private TableView<Result> tableView;

    @Override
    public void start(Stage primaryStage) {

        Label instructionLabel = new Label("Voer maximaal 2 keywords in, gescheiden door spaties:");


        TextField keywordField = new TextField();
        keywordField.setPromptText("Bijv. Developer Amsterdam");


        Button searchButton = new Button("Zoek");


        VBox inputBox = new VBox(5, instructionLabel, keywordField, searchButton);
        inputBox.setPadding(new Insets(10));


        tableView = new TableView<>();


        TableColumn<Result, String> nameColumn = new TableColumn<>("Naam");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("personName"));
        nameColumn.setMinWidth(200);


        TableColumn<Result, Integer> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreColumn.setMinWidth(100);

        tableView.getColumns().addAll(nameColumn, scoreColumn);


        tableView.setRowFactory(tv ->
        {
            TableRow<Result> row = new TableRow<>();
            row.setOnMouseClicked(event ->
            {
                if (!row.isEmpty() &&
                        event.getButton() == MouseButton.PRIMARY &&
                        event.getClickCount() == 2)
                {
                    Result clickedResult = row.getItem();
                    UserPerson person = clickedResult.getPerson();

                    showAlert(Alert.AlertType.INFORMATION, "Contactgegevens",
                            "Naam: " + person.getNaam() + "\n" +
                                    "Functie: " + person.getFunctie() + "\n" +
                                    "Locatie: " + person.getLocatie() + "\n" +
                                    "Contact: " + person.getContact());
                }
            });
            return row;
        });


        BorderPane root = new BorderPane();
        root.setTop(inputBox);
        root.setCenter(tableView);


        searchButton.setOnAction(e -> {
            String input = keywordField.getText().trim();
            if (input.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Fout", "Voer minstens één keyword in.");
                return;
            }
            String[] keywords = input.split("\\s+");
            if (keywords.length > 2) {
                showAlert(Alert.AlertType.WARNING, "Ongeldige invoer", "Voer maximaal 2 woorden in.");
                return;
            }


            SearchService service = new SearchService(new BasicKeywordMatcher(), new DataLoader());
            List<Result> results = service.search("Data.json", keywords);

            if (results.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Resultaat", "Geen resultaten gevonden.");
            } else {
                updateTable(results);
            }
        });

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Overzicht Business Partners");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private void updateTable(List<Result> results) {
        ObservableList<Result> data = FXCollections.observableArrayList(results);
        tableView.setItems(data);
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }




    public static class UserPerson {
        private String naam;
        private String functie;
        private String locatie;
        private String posts;
        private String photo;
        private String contact;

        public UserPerson() { }

        public UserPerson(String naam, String functie, String locatie, String posts, String photo, String contact) {
            this.naam = naam;
            this.functie = functie;
            this.locatie = locatie;
            this.posts = posts;
            this.photo = photo;
            this.contact = contact;
        }

        public String getNaam() { return naam; }
        public String getFunctie() { return functie; }
        public String getLocatie() { return locatie; }
        public String getPosts() { return posts; }
        public String getPhoto() { return photo; }
        public String getContact() { return contact; }
    }


    public static class Result {
        private UserPerson person;
        private int score;

        public Result(UserPerson person, int score) {
            this.person = person;
            this.score = score;
        }

        public UserPerson getPerson() { return person; }
        public int getScore() { return score; }


        public String getPersonName() {
            return person != null ? person.getNaam() : "";
        }
    }


    public interface KeywordMatcher {
        int match(UserPerson person, String[] keywords);
    }


    public static class BasicKeywordMatcher implements KeywordMatcher {
        @Override
        public int match(UserPerson person, String[] keywords) {
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
            if (text == null || keyword == null || keyword.isEmpty())
                return 0;
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


    public static class DataLoader {
        public List<UserPerson> load(String jsonFileName) {
            try {
                String jsonContent = Files.readString(Paths.get(jsonFileName));
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonContent, new TypeReference<List<UserPerson>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
    }


    public static class SearchService {
        private final KeywordMatcher matcher;
        private final DataLoader loader;

        public SearchService(KeywordMatcher matcher, DataLoader loader) {
            this.matcher = matcher;
            this.loader = loader;
        }

        public List<Result> search(String fileName, String[] keywords) {
            List<UserPerson> persons = loader.load(fileName);
            List<Result> results = new ArrayList<>();
            for (UserPerson person : persons) {
                int score = matcher.match(person, keywords);
                if (score > 0) {
                    results.add(new Result(person, score));
                }
            }

            results.sort(Comparator.comparing(Result::getScore).reversed());
            return results;
        }
    }
}
