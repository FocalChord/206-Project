package A3.NameSayer.Backend.Databases;

import A3.NameSayer.Backend.Items.DatabaseName;
import A3.NameSayer.Backend.Items.DatabaseNameProperties;
import A3.NameSayer.Backend.RatingSystem.Rating;
import A3.NameSayer.Backend.RatingSystem.TextFileRW;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Database {
    public static final String NO_RATING = "No Rating Yet";
    public static final String GOOD_RATING = "Good";
    public static final String BAD_RATING = "Bad";
    private static final String PATH_TO_NAMES_FOLDER = new File("Databases/names").getAbsolutePath();

    private static Database _database;

    private TextFileRW _textFileRW = TextFileRW.getInstance();

    private List<DatabaseName> _databaseNameList = FXCollections.observableArrayList();
    private ObservableList<DatabaseNameProperties> _tableList = FXCollections.observableArrayList();

    private DatabaseNameProperties _currentlySelectedName = null;

    private File[] files = new File(PATH_TO_NAMES_FOLDER).listFiles();
    private List<File> fileList = new ArrayList<>(Arrays.asList(files));


    // Initializing and reading from he database
    private Database() {
        initialiseDBNameList();
        initialiseTableList();
    }

    // Putting the singleton in "singleton"
    public static Database getInstance() {
        if (_database == null) {
            _database = new Database();
        }

        return _database;
    }

    // Public Methods

    public void setCurrentDatabaseName(DatabaseNameProperties db) {
        _currentlySelectedName = db;
    }

    public void changeRating(Rating rating) {
        _textFileRW.changeRating(getDatabaseObj(_currentlySelectedName.getDBName()), rating);

        // Updates the table view
        getCorrespondingPropertyObj()
                .dbRatingProperty()
                .setValue(getDatabaseObj(_currentlySelectedName.getDBName()).getStringRating(true));
    }

    public ObservableList<DatabaseNameProperties> getDatabaseTableList() {
        return _tableList;
    }

    public FilteredList<String> getDatabaseNameList() {
        ObservableList<String> out = FXCollections.observableArrayList();
        _databaseNameList.stream().map(DatabaseName::getName).forEach(out::add);
        return new FilteredList<>(out, s -> true);
    }

    public DatabaseName getDatabaseObj(String databaseName) {
        return _databaseNameList.stream()
                .filter(e -> e.getName().equals(databaseName))
                .findAny()
                .orElse(null);
    }

    // Internal Methods

    private DatabaseNameProperties getCorrespondingPropertyObj() {
        return _tableList.stream()
                .filter(e -> e.getDBName().equals(getDatabaseObj(_currentlySelectedName.getDBName()).getName()))
                .findAny()
                .orElse(null);
    }

    private void filterAndSort() {
        Set<String> nameSet = new HashSet<>();
        _databaseNameList = _databaseNameList.stream()
                .filter(e -> nameSet.add(e.getName()))
                .sorted(Comparator.comparing(DatabaseName::getName))
                .collect(Collectors.toList());
    }

    private void processList(List<String> list) {
        Pattern p = Pattern.compile(": [A-Za-z]+( \\(\\d+\\))?,");

        for (String s : list) {
            System.out.println(s);
            Matcher m1 = p.matcher(s);
            String tempName = "";
            String tempRating = "";
            while (m1.find()) {
                tempName = m1.group();
            }
            tempName = tempName.substring(2);
            tempName = tempName.substring(0, tempName.length() - 1);

            // Rating is either good or bad
            tempRating = s.substring((s.length() - 4));

            // Case where the rating is bad
            if (tempRating.substring(0, 1).equals(" ")) {
                tempRating = tempRating.substring(1);
            }

            if (tempRating.length() == 4) {
                getDatabaseObj(tempName).setRating(Rating.GOOD);
            }

            if (tempRating.length() == 3) {
                getDatabaseObj(tempName).setRating(Rating.BAD);
            }


        }
    }

    private void initialiseDBNameList() {
        for (File f : fileList) {
            _databaseNameList.add(new DatabaseName(f));
        }

        readFromRatingFile();
        filterAndSort();
    }

    private void readFromRatingFile() {
        File goodTxtFile = new File(TextFileRW.GOOD_TXT_FILE_NAME);
        File badTxtFile = new File(TextFileRW.BAD_TXT_FILE_NAME);

        List<String> goodList;
        List<String> badList;

        try {
            if (goodTxtFile.exists()) {
                goodList = Files.readAllLines(goodTxtFile.toPath(), Charset.forName("UTF-8"));
                processList(goodList);
            }

            if (badTxtFile.exists()) {
                badList = Files.readAllLines(badTxtFile.toPath(), Charset.forName("UTF-8"));
                processList(badList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void initialiseTableList() {
        for (DatabaseName db : _databaseNameList) {
            _tableList.add(new DatabaseNameProperties(db));
        }
    }
}
