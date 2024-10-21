package uob.oop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;


public class Toolkit {
    public static List<String> listVocabulary = null;
    public static List<double[]> listVectors = null;
    private static final String FILENAME_GLOVE = "glove.6B.50d_Reduced.csv";

    public static final String[] STOPWORDS = {"a", "able", "about", "across", "after", "all", "almost", "also", "am", "among", "an", "and", "any", "are", "as", "at", "be", "because", "been", "but", "by", "can", "cannot", "could", "dear", "did", "do", "does", "either", "else", "ever", "every", "for", "from", "get", "got", "had", "has", "have", "he", "her", "hers", "him", "his", "how", "however", "i", "if", "in", "into", "is", "it", "its", "just", "least", "let", "like", "likely", "may", "me", "might", "most", "must", "my", "neither", "no", "nor", "not", "of", "off", "often", "on", "only", "or", "other", "our", "own", "rather", "said", "say", "says", "she", "should", "since", "so", "some", "than", "that", "the", "their", "them", "then", "there", "these", "they", "this", "tis", "to", "too", "twas", "us", "wants", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with", "would", "yet", "you", "your"};

    public void loadGlove() throws IOException {
        BufferedReader myReader = null;
        listVocabulary = new ArrayList<>();
        listVectors = new ArrayList<>();

        try {
            myReader = new BufferedReader(new FileReader(Toolkit.getFileFromResource(FILENAME_GLOVE)));
            String content;

            while ((content = myReader.readLine()) != null) {
                String [] splitContent = content.split(",");
                listVocabulary.add(splitContent[0]);
                double [] vectorArray = new double[splitContent.length - 1];

                for (int i = 1; i < splitContent.length; i++) {
                    vectorArray[i - 1] = Double.parseDouble(splitContent[i]);
                }
                listVectors.add(vectorArray);
            }
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
        } finally {
            if (myReader != null) {
                try {
                    myReader.close();
                } catch (IOException exception) {
                    System.err.println(exception.getMessage());
                }
            }
        }
    }


    private static File getFileFromResource(String fileName) throws URISyntaxException {
        ClassLoader classLoader = Toolkit.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException(fileName);
        } else {
            return new File(resource.toURI());
        }
    }

    public List<NewsArticles> loadNews() {
        List<NewsArticles> listNews = new ArrayList<>();
        try {
            File folder = Toolkit.getFileFromResource("News");
            File [] files = folder.listFiles();
            for (int i = 0; i < files.length - 1; i++) {
                for (int j = 0; j < files.length - 1 - i; j++) {
                    if (files[j].getName().compareTo(files[j + 1].getName()) > 0) {
                        File swap = files[j];
                        files[j] = files[j + 1];
                        files[j + 1] = swap;
                    }
                }
            }
            if (files != null) {
                for (File currentFile : files) {
                    if (currentFile.getName().toLowerCase().endsWith(".htm")) {
                        StringBuilder HTMLcontent = new StringBuilder();
                        try (BufferedReader myReader2 = new BufferedReader(new FileReader(currentFile))) {
                            String currentLine;
                            while ((currentLine = myReader2.readLine()) != null) {
                                HTMLcontent.append(currentLine).append(System.lineSeparator());
                            }
                        }
                        String currentTitle = HtmlParser.getNewsTitle(HTMLcontent.toString());
                        String currentLabel = HtmlParser.getLabel(HTMLcontent.toString());
                        String currentContent = HtmlParser.getNewsContent(HTMLcontent.toString());
                        NewsArticles.DataType dataType = HtmlParser.getDataType(HTMLcontent.toString());
                        NewsArticles newsArticles = new NewsArticles(currentTitle, currentContent, dataType, currentLabel);
                        listNews.add(newsArticles);
                    }
                    }
                }
            } catch (IOException | URISyntaxException exception) {
            System.out.println(exception.getMessage());
        }
        return listNews;
    }

    public static List<String> getListVocabulary() {
        return listVocabulary;
    }

    public static List<double[]> getlistVectors() {
        return listVectors;
    }
}
