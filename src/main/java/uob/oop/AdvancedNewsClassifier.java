package uob.oop;

import org.apache.commons.lang3.time.StopWatch;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdvancedNewsClassifier {
    public Toolkit myTK = null;
    public static List<NewsArticles> listNews = null;
    public static List<Glove> listGlove = null;
    public List<ArticlesEmbedding> listEmbedding = null;
    public MultiLayerNetwork myNeuralNetwork = null;

    public final int BATCHSIZE = 10;

    public int embeddingSize = 0;
    private static StopWatch mySW = new StopWatch();

    public AdvancedNewsClassifier() throws IOException {
        myTK = new Toolkit();
        myTK.loadGlove();
        listNews = myTK.loadNews();
        listGlove = createGloveList();
        listEmbedding = loadData();
    }

    public static void main(String[] args) throws Exception {
        mySW.start();
        AdvancedNewsClassifier myANC = new AdvancedNewsClassifier();

        myANC.embeddingSize = myANC.calculateEmbeddingSize(myANC.listEmbedding);
        myANC.populateEmbedding();
        myANC.myNeuralNetwork = myANC.buildNeuralNetwork(2);
        myANC.predictResult(myANC.listEmbedding);
        myANC.printResults();
        mySW.stop();
        System.out.println("Total elapsed time: " + mySW.getTime());
    }

    public List<Glove> createGloveList() {
        List<Glove> listResult = new ArrayList<>();
        for (int i = 0; i < Toolkit.listVocabulary.size(); i++) {
            String currentWord = Toolkit.listVocabulary.get(i);
            double[] currentVector = Toolkit.listVectors.get(i);

            boolean isStopWord = false;

            for (String stopWord : Toolkit.STOPWORDS) {
                if (stopWord.equals(currentWord)) {
                    isStopWord = true;
                    break;
                }
            }

            if (!isStopWord) {
                Vector currentVector2 = new Vector(currentVector);
                Glove newGlove = new Glove(currentWord, currentVector2);
                listResult.add(newGlove);
            }
        }
        return listResult;
    }


    public static List<ArticlesEmbedding> loadData() {
        List<ArticlesEmbedding> listEmbedding = new ArrayList<>();
        for (NewsArticles news : listNews) {
            ArticlesEmbedding myAE = new ArticlesEmbedding(news.getNewsTitle(), news.getNewsContent(), news.getNewsType(), news.getNewsLabel());
            listEmbedding.add(myAE);
        }
        return listEmbedding;
    }

    public int calculateEmbeddingSize(List<ArticlesEmbedding> _listEmbedding) {
        int intMedian = -1;
        List<Integer> lengthOfDocument = getLengthOfDocument(_listEmbedding);
        lengthOfDocument.sort(null);
        int currentSize = lengthOfDocument.size();
        if (currentSize % 2 == 0) {
            int medianOne = lengthOfDocument.get(currentSize / 2);
            int medianTwo = lengthOfDocument.get((currentSize / 2) + 1);
            intMedian = (medianOne + medianTwo) / 2;
        } else {
            intMedian = lengthOfDocument.get((currentSize / 2) + 1);
        }
        return intMedian;
    }

    private static List<Integer> getLengthOfDocument(List<ArticlesEmbedding> _listEmbedding) {
        List<Integer> lengthOfDocument = new ArrayList<>();
        for (ArticlesEmbedding value : _listEmbedding) {
            String currentContent = value.getNewsContent();
            String[] splitWords = currentContent.split(" ");
            int wordOccurance = 0;
            for (String currentWord : splitWords) {
                if (Toolkit.listVocabulary.contains(currentWord)) {
                    wordOccurance++;
                }
            }
            int docLength = wordOccurance;
            lengthOfDocument.add(docLength);
        }

        return lengthOfDocument;
    }

    public void populateEmbedding() {
        for (ArticlesEmbedding newEmbedding : listEmbedding) {
            try {
                newEmbedding.getEmbedding();
            } catch (InvalidSizeException exception) {
                newEmbedding.setEmbeddingSize(embeddingSize);
            } catch (InvalidTextException exception) {
                newEmbedding.getNewsContent();
            } catch (Exception exception) {
            }
        }
    }


    public DataSetIterator populateRecordReaders(int _numberOfClasses) throws Exception {
        ListDataSetIterator myDataIterator = null;
        List<DataSet> listDS = new ArrayList<>();
        INDArray inputNDArray = null;
        INDArray outputNDArray = null;

        for (ArticlesEmbedding currentArticle : listEmbedding) {
            if (currentArticle.getNewsType().equals(NewsArticles.DataType.Training)) {
                inputNDArray = currentArticle.getEmbedding();
                outputNDArray = Nd4j.zeros(1, _numberOfClasses);
                int labelNumber = Integer.parseInt(currentArticle.getNewsLabel()) - 1;
                if (labelNumber >= 0 && labelNumber < _numberOfClasses) {
                    outputNDArray.putScalar(0, labelNumber, 1);
                    DataSet myDataSet = new DataSet(inputNDArray, outputNDArray);
                    listDS.add(myDataSet);
                }
            }
        }
        return new ListDataSetIterator(listDS, BATCHSIZE);
    }

    public MultiLayerNetwork buildNeuralNetwork(int _numOfClasses) throws Exception {
        DataSetIterator trainIter = populateRecordReaders(_numOfClasses);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(42)
                .trainingWorkspaceMode(WorkspaceMode.ENABLED)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .updater(Adam.builder().learningRate(0.02).beta1(0.9).beta2(0.999).build())
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(embeddingSize).nOut(15)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.HINGE)
                        .activation(Activation.SOFTMAX)
                        .nIn(15).nOut(_numOfClasses).build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        for (int n = 0; n < 100; n++) {
            model.fit(trainIter);
            trainIter.reset();
        }
        return model;
    }

    public List<Integer> predictResult(List<ArticlesEmbedding> _listEmbedding) throws Exception {
        List<Integer> listResult = new ArrayList<>();
        for (ArticlesEmbedding currentData : _listEmbedding) {
            if (currentData.getNewsType().equals(NewsArticles.DataType.Testing)) {
                INDArray inputArray = currentData.getEmbedding();
                int[] outputArray = myNeuralNetwork.predict(inputArray);
                if (outputArray.length > 0) {
                    int currentLabelNumber = outputArray[0];
                    listResult.add(currentLabelNumber);
                    currentData.setNewsLabel(Integer.toString(currentLabelNumber));
                }
            }
        }
        return listResult;
    }

    public void printResults() {
        int highestLabel = 1;
        for (ArticlesEmbedding currentData : listEmbedding) {
            if (currentData.getNewsType().equals(NewsArticles.DataType.Testing)) {
                int currentLabel =  Integer.parseInt(currentData.getNewsLabel());
                highestLabel = Math.max(highestLabel, currentLabel);
            }
        }
        int groupNumbers = highestLabel + 1;
        List<String> [] printedResults = new List[groupNumbers];
        for (ArticlesEmbedding currentData : listEmbedding) {
            if (currentData.getNewsType().equals(NewsArticles.DataType.Testing)) {
                int currentLabel =  Integer.parseInt(currentData.getNewsLabel());


                if (currentLabel >= 0 && currentLabel < groupNumbers) {
                    if (printedResults[currentLabel] == null) {
                        printedResults[currentLabel] = new ArrayList<>();
                    }
                    printedResults[currentLabel].add(currentData.getNewsTitle());
                }
            }
        }
        for (int i = 0; i < groupNumbers; i++) {
            System.out.println("Group " + (i + 1));
            if (printedResults[i] != null) {
                for (String finalResult : printedResults[i]) {
                    System.out.println(finalResult);
                }
            }
        }
    }
}
