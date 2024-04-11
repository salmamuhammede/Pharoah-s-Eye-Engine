/*import java.io.BufferedReader;
import java.io.FileReader;*/
import java.io.IOException;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.HashMap;
import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;

/*import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;*/
import static com.mongodb.client.model.Filters.eq;

import com.mongodb.*;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.*;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;

public class Ranker {

    public static Set<String> stopWords = new HashSet<>();
    public static MongoDatabase db;
    public static MongoCollection<org.bson.Document> DocCollection;
    public static MongoCollection<org.bson.Document> WordsCollection;
    public static MongoCollection<org.bson.Document> WordDocCollection;
    public static Database DBhandler;
    

    public static void main(String[] args) throws IOException {
        String query = "Search for documents about artificial intelligence and its impact on society."; ///to be modifed get from react
        //ArrayList<String> afterProcessing = QueryProcessing(Query);
        String path="src/main/java/Stopwords.txt";//Step1 extracting StopWords
        try {
            getStopwords(path);
            ArrayList<String> afterProcessing = queryProcessing(query);
            HashSet<String> uniqueTerms = new HashSet<>(afterProcessing);


            //////////////////////////////////////////////tested before calling the database
            MongoClient client = createConnection();
            if (client == null) {
                System.err.println("Failed to create connection");
                return;
            }

            ObjectId id = new ObjectId("66172eb9eb21ba702f7a9a37");
            Document document = getWordDoc(client, id);

            if (document != null) {
                HashMap<String, Object> DocWordData = DocWordData(document);
                System.out.println(DocWordData);
            } else {
                System.out.println("Failed to retrieve document data");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //HashMap<String, Object> DocWordData=new HashMap<>(document);
        //System.out.println(DocWordData);*/




    }
    public static MongoClient createConnection() {
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");
        try {
            MongoClient client = MongoClients.create(connectionString);
            return client;
        } catch (MongoException e) {
            System.err.println("Error creating connection: " + e.getMessage());
            return null;
        }
    }

    public static Document getWordDoc(MongoClient client, ObjectId id) {
        try (client) {  // Ensure connection is closed after use
            db = client.getDatabase("test");
            WordDocCollection = db.getCollection("Word_Document");  // Not needed here

            Document document = WordDocCollection.find(new Document("_id", id)).first();
            if (document == null) {
                System.err.println("Document not found with ID: " + id);
                return null;
            }
            return document;
        } catch (MongoException e) {
            System.err.println("Error retrieving document: " + e.getMessage());
            return null;
        }
    }
    public static ArrayList<String>queryProcessing(String query) throws IOException {
        ArrayList<String>datbseQuery=new ArrayList<>();
        query = query.toLowerCase();//to easyitfor stopWords Removal
        // 2. Tokenize the query (split into words)
        ArrayList<Document>queryInfo=new ArrayList<>();
        String[] tokens = query.split("\\s+");

        for (String token : tokens) {
            try {
                token = removeStopWords(token);
                //System.out.println(token);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                token = Stemming(token);
                //System.out.println(token);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //System.out.println(token);
            datbseQuery.add(token);
        }
        System.out.println("Tokens of the query:");
        for (String token : datbseQuery) {
            System.out.println(token);
        }
        return datbseQuery;
    }
    public static void getStopwords(String filePath) throws IOException {
        try {
            Scanner scanner = new Scanner(new File(filePath));

            while (scanner.hasNext()) {
                String word = scanner.nextLine().trim();
                stopWords.add(word);
            }

            scanner.close();

           // System.out.println("Unique words in the file:");
            //System.out.println(stopWords);

        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found at " + filePath);
        }
    }
    private static String removeStopWords(String input) throws IOException {
        input = input.replaceAll("[^a-zA-Z0-9]", " ").replaceAll("\\s+", " ").trim();
        StringBuffer stopWordResult = new StringBuffer(" ");
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (!stopWords.contains(word.toLowerCase())) {
                stopWordResult.append(word).append(" ");
            }
        }
        return stopWordResult.toString();
    }

    private static String Stemming(String input) throws IOException {
        PorterStemmer porterStemmerr = new PorterStemmer();
        StringBuffer stemmingResult = new StringBuffer(" ");
        String[] words = input.split("\\s+");
        for (String word : words) {
            stemmingResult.append(porterStemmerr.stem(word)).append(" ");
        }
        return stemmingResult.toString();
    }
    /////////////////////////////////////////////////////////
    private static double getInfo()
    {
        double score=0.0;
        return score;
    }
    private static HashMap<String, Object> processWordDocument(Document document) {
        HashMap<String, Object> details = new HashMap<>();
        details.put("word", document.getString("word"));

        // Access IDF as a number (assuming it's stored as a double)
        double idf = document.getDouble("idf");
        details.put("idf", idf);

        // Access the ArrayList of integers (assuming it's stored as an array)
        List<Integer> ids = (List<Integer>) document.get("ids");
        details.put("ids", ids);

        // Add more details as needed based on your document schema
        return details;
    }
    private static HashMap<String, Object> DocWordData(Document document) {
        HashMap<String, Object> details = new HashMap<>();
        details.put("_id", document.getObjectId("_id"));

        // Access IDF as a number (assuming it's stored as a double)
        ObjectId Docid = document.getObjectId("Docid");
        details.put("Docid", Docid);
        Integer tf = document.getInteger("tf");
        details.put("tf", tf);

        // Access the ArrayList of integers (assuming it's stored as an array)
        List<Integer> Positions = (List<Integer>) document.get("Positions");
        details.put("Positions", Positions);

        // Add more details as needed based on your document schema
        return details;
    }
   /* private  static ArrayList<String> QueryProcessing(String Query)
    {
        //Stemming
        //removeStopWordds
        //Quereyparsing
    }
    private static ArrayList<HashMap<String,Integer>>Convert(ArrayList<String>arr)
    {
        
    }*/


}
