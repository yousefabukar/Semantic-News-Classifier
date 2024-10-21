package uob.oop;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.Properties;


public class ArticlesEmbedding extends NewsArticles {
    private int intSize = -1;
    private String processedText = "";

    private INDArray newsEmbedding = Nd4j.create(0);

    public ArticlesEmbedding(String _title, String _content, NewsArticles.DataType _type, String _label) {
        super(_title,_content,_type,_label);
    }

    public void setEmbeddingSize(int _size) {
        this.intSize = _size;
    }

    public int getEmbeddingSize(){
        return intSize;
    }

    @Override
    public String getNewsContent() {
        if (processedText.isEmpty()) {
            String currentContent = super.getNewsContent();
            currentContent = textCleaning(currentContent);
            currentContent = lemmatizeText(currentContent);
            currentContent = removeStopWords(currentContent);
            processedText = currentContent.toLowerCase();
        }
        return processedText.trim();
    }
    private String lemmatizeText(String currentContent) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,pos,lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument document = pipeline.processToCoreDocument(currentContent);

        StringBuilder sbContent = new StringBuilder();
        for (CoreLabel tok : document.tokens()) {
            sbContent.append(tok.lemma()).append(" ");
        }

        return sbContent.toString().trim();
    }
    private String removeStopWords(String currentContent) {
        String[] stopWords = Toolkit.STOPWORDS;
        StringBuilder sbContent = new StringBuilder();
        String[] splitWords = currentContent.split(" ");

        for (String word : splitWords) {
            boolean isStopWord = false;
            for (String stopWord : stopWords) {
                if (word.equals(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }
            if (!isStopWord) {
                sbContent.append(word).append(" ");
            }
        }
        return sbContent.toString();
    }

    public INDArray getEmbedding() throws Exception {

        intSize = getEmbeddingSize();
        if (intSize == -1) {
            throw new InvalidSizeException("Invalid size");
        }
        if (processedText.isEmpty()) {
            throw new InvalidTextException("Invalid text");
        }
        if (newsEmbedding.isEmpty()) {
            newsEmbedding = Nd4j.create(intSize, 50);
            String[] wordsInText = processedText.split(" ");
            int openRow = 0;

            for (int i = 0; i < wordsInText.length; i++) {
                String currentText = wordsInText[i];

                for (int j = 0; j < AdvancedNewsClassifier.listGlove.size(); j++) {
                    Glove newGlove = AdvancedNewsClassifier.listGlove.get(j);
                    if (newGlove.getVocabulary().equals(currentText) && openRow < intSize) {
                        Vector wordVector = newGlove.getVector();
                        INDArray newArray = Nd4j.create(wordVector.getAllElements());
                        newsEmbedding.putRow(openRow, newArray);
                        openRow++;
                        break;
                    }
                }
                if (openRow >= intSize) {
                    break;
                }
            }
            while (openRow < intSize) {
                newsEmbedding.putRow(openRow, Nd4j.zeros(1, 50));
                openRow++;
            }
        }
        return Nd4j.vstack(newsEmbedding.mean(1));
    }
    /***
     * Clean the given (_content) text by removing all the characters that are not 'a'-'z', '0'-'9' and white space.
     * @param _content Text that need to be cleaned.
     * @return The cleaned text.
     */
    private static String textCleaning(String _content) {
        StringBuilder sbContent = new StringBuilder();

        for (char c : _content.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || Character.isWhitespace(c)) {
                sbContent.append(c);
            }
        }

        return sbContent.toString().trim();
    }
}
