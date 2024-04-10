import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Indexer {
    //data structures
    private static BufferedReader reader; //read links
    public static Set<String> stopwords = new HashSet<>();
    public static ArrayList<HashMap<String, Integer>> pos_wrd_Cnt;  // word appeard in position x n time in one doc

    public static HashMap<String,Integer> WordDocsCount;   // words apeared in n docs

    public static HashMap<String, ArrayList<ObjectId>> DocsOfWord = new HashMap<String, ArrayList<ObjectId>>();// words apeared in n docs

    private static final int POSITIONS_SIZE = 7;
    ///////  DataBase  /////
    public static MongoDatabase db;
    public static MongoCollection<org.bson.Document> DocCollection;
    public static MongoCollection<org.bson.Document> WordsCollection;
    public static MongoCollection<org.bson.Document> WordDocCollection;
    public static Database DBhandler;
    private static int lengthOfDocument;

    private static int popularity;

    public static void main(String[] args) throws IOException {
        try {
            reader = new BufferedReader(new FileReader("output.txt"));
        } catch (IOException e) {
            System.err.println("Error initializing BufferedReader: " + e.getMessage());
        }
        //readstopwords in hashset
        fillSetFromFile("D:\\Pharoah_eye_project\\Pharoah-s-Eye-Engine\\java\\Web-Crawler\\src\\main\\java\\Stopwords.txt");
        DBhandler=new Database();
        initDataBase();
        String url = readNextLine();
        Document document;
        while (url != null) {
            lengthOfDocument=0;
            document = Jsoup.connect(url).get();
            handler(document);
            ObjectId docid=DBhandler.insertDocument(DocCollection,url,Optional.of(popularity));
            PassWordsToDB(docid);
            pos_wrd_Cnt.clear();
            url = readNextLine();
        }
        insertAllWords();
        closeReader();
    }

    public static void handler(Document document) {
        //priority: title> high_headers> mid_headers> low_headers> body> bolds> leftovers
        String title = document.select("title").text();
        String high_headers = document.select("h1,h2").text();
        String mid_headers = document.select("h3,h4").text();
        String low_headers = document.select("h5,h6").text();
        String body = document.select("p,span,code,textarea").text();
        String bolds = document.select("b,strong,i,em,blockquote").text();
        // Selecting leftovers: all text nodes not within tags covered above
        String leftovers = document.select("a,ul, ol, li, table, tr, td, th, form").text();
        String[] arr = {title, high_headers, mid_headers, low_headers, body, bolds, leftovers};
        //remove stop words and stem them

        for (int i = 0; i < arr.length; i++) {
            //remove stopping words
            try {
                arr[i] = removeStopwords(arr[i]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                arr[i] = Stemming(arr[i]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        filler(arr);
    }

    public static void filler(String[] arr) {
        pos_wrd_Cnt = new ArrayList<HashMap<String, Integer>>();

        // Initialize a HashMap to store counts of all words
        HashMap<String, Integer> allWordsCount = new HashMap<>();

        // Loop through each position
        for (int i = 0; i < POSITIONS_SIZE; i++) {
            // Initialize a new HashMap for the current position
            HashMap<String, Integer> wordCountMap = new HashMap<>();
            pos_wrd_Cnt.add(wordCountMap);

            // Split the input string into words
            String[] words = arr[i].split("\\s+");

            // Update counts for words in the current position
            for (String word : words) {
                // Update count for the current position
                int count = wordCountMap.getOrDefault(word, 0);
                wordCountMap.put(word, count + 1);

                // Update count for all words
                lengthOfDocument++;
                count = allWordsCount.getOrDefault(word, 0);
                allWordsCount.put(word, count + 1);
            }
        }

        // Store the counts of all words in the last position of pos_wrd_Cnt
        pos_wrd_Cnt.add(allWordsCount);

        for(int i=0;i<=POSITIONS_SIZE;i++)
        {
            pos_wrd_Cnt.get(i).remove("");
        }
    }


    public static String readNextLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("Error reading from file: " + e.getMessage());
            return null;
        }
    }

    public static void closeReader() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing BufferedReader: " + e.getMessage());
        }
    }

    public static void fillSetFromFile(String filename) throws IOException {
        BufferedReader stopreader = new BufferedReader(new FileReader(filename));
        String line;

        // Read each line from the file and add it to the set
        while ((line = stopreader.readLine()) != null) {
            stopwords.add(line);
        }

        stopreader.close();
    }


    public static String removeStopwords(String input) throws IOException {
        input = input.replaceAll("[^a-zA-Z0-9]", " ").replaceAll("\\s+", " ").trim();
        StringBuffer result = new StringBuffer(" ");
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (!stopwords.contains(word.toLowerCase())) {
                result.append(word).append(" ");
            }
        }
        return result.toString();
    }

    public static String Stemming(String input) throws IOException {
        PorterStemmer porterStemmer = new PorterStemmer();
        StringBuffer result = new StringBuffer(" ");
        String[] words = input.split("\\s+");
        for (String word : words) {
            result.append(porterStemmer.stem(word)).append(" ");
        }
        return result.toString();
    }

    public static void initDataBase() {
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");

        try (MongoClient client = MongoClients.create(connectionString)) {
            db = client.getDatabase("test");
            DocCollection = db.getCollection("documents");
            WordsCollection = db.getCollection("Words");
            WordDocCollection = db.getCollection("Word_Document");

            System.out.println("Connected to database");

            // Create unique index on the "_id" field
            IndexOptions indexOptions = new IndexOptions();
            DocCollection.createIndex(Indexes.ascending("_id"), indexOptions);
            WordsCollection.createIndex(Indexes.ascending("_id"), indexOptions);
            WordDocCollection.createIndex(Indexes.ascending("_id"), indexOptions);
        }
    }
    // ArrayOfdocs;   pos_wrd_Cnt;
    public static void PassWordsToDB(ObjectId docid) {
        ArrayList<Integer> PositionOfWord=new ArrayList<Integer>(POSITIONS_SIZE);
        String Word;
        float TF;
        ObjectId ReturnedId;
        for(int i=0;i<pos_wrd_Cnt.get(POSITIONS_SIZE).size();i++)
        {
            Word=pos_wrd_Cnt.get(POSITIONS_SIZE).entrySet().iterator().next().getKey();
            TF=pos_wrd_Cnt.get(POSITIONS_SIZE).entrySet().iterator().next().getValue()/(float)lengthOfDocument;
            for(int j=0;j<POSITIONS_SIZE;j++)
            {
                PositionOfWord.set(j, pos_wrd_Cnt.get(i).get(Word));
                pos_wrd_Cnt.get(i).remove(Word);
            }
            ReturnedId=DBhandler.insert_DocWordData(WordDocCollection,PositionOfWord,TF,docid);
            addValueToArrayList(Word,ReturnedId);
        }

    }
    // Retrieve the ArrayList associated with the key
    public static void addValueToArrayList(String key, ObjectId value) {
        // Retrieve the ArrayList associated with the key
        ArrayList<ObjectId> arrayList = DocsOfWord.getOrDefault(key, new ArrayList<>());
        // Add the new value to the ArrayList
        arrayList.add(value);

        // Put the updated ArrayList back into the HashMap
        DocsOfWord.put(key, arrayList);
        WordDocsCount.put(key,WordDocsCount.getOrDefault(key, 0)+1);
    }
    public static void insertAllWords(){
        String Word;
        int NumberOfDocs;
        double IDF;
        ArrayList<ObjectId> arr;
        for(int i=0;i<WordDocsCount.size();i++)
        {
            Word=WordDocsCount.entrySet().iterator().next().getKey();
            NumberOfDocs=WordDocsCount.entrySet().iterator().next().getValue();
            IDF=Math.log((double) Crawler.MAX_QUEUE_SIZE / NumberOfDocs);
            arr=DocsOfWord.get(Word);
            DBhandler.Wordinsertion(WordsCollection,Word,NumberOfDocs,IDF,arr);
        }
    }

}


