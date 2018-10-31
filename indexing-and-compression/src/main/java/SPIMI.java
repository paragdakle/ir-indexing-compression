package main.java;

import main.java.compression.CompressionManager;
import main.java.model.PostingFileItem;
import main.java.nlp.Tokenizer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SPIMI {

    private Map<String, LinkedList<PostingFileItem>> index = null;
    private String[] docIds;
    private int[] maxDocTermFrequency;
    private int maxTermLength = -1;
    private static HashSet<String> stopWords;

    private void createIndex(Map<String, List<String>> termMap, String outFile) {
        docIds = new String[termMap.size() + 1];
        int counter = 1;
        docIds[0] = "";

        for(String docId: termMap.keySet()) {
            try {
                if(!docIds[counter - 1].equals(docId)) {
                    docIds[counter] = docId;
                }
                List<String> termList = termMap.get(docId);
                if(index == null) {
                    index = new LinkedHashMap<>();
                }
                for(String term: termList) {
                    if(stopWords.contains(term)) {
                        continue;
                    }
                    if(maxTermLength < term.length()) {
                        maxTermLength = term.length();
                    }
                    if(index.containsKey(term)) {
                        LinkedList<PostingFileItem> postingList = index.get(term);
                        PostingFileItem postingFileItem = new PostingFileItem(counter);
                        int itemIndex = postingList.indexOf(postingFileItem);
                        if(itemIndex > -1) {
                            postingList.get(itemIndex).incrementTermFrequency();
                        }
                        else {
                            postingList.add(postingFileItem);
                        }
                    }
                    else {
                        PostingFileItem postingFileItem = new PostingFileItem(counter);
                        LinkedList<PostingFileItem> postingList = new LinkedList<>();
                        postingList.add(postingFileItem);
                        index.put(term, postingList);
                    }
                }
                counter++;
            }
            catch (OutOfMemoryError e) {
                System.out.println(e.getMessage());
            }
        }
        sortIndex();
        writeIndexToRandomAccessFile(outFile);
        writeIndexToFile(outFile + ".txt");
    }

    public static void loadStopwords(String stopWordsFilePath) {
        try {
            stopWords = new HashSet<>(1);
            File file = new File(stopWordsFilePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line.toLowerCase().trim());
            }
            reader.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        if(args.length != 2 || args[0].isEmpty()) {
            System.out.println("Error: Invalid number of arguments found!");
            System.out.println("Expected:");
            System.out.println("java SPIMI <corpus_dir_path> <stopwords_filepath>");
            System.exit(0);
        }

        loadStopwords(args[1]);
        createLemmaIndexes(args[0], args[1], "index_version1.uncompress", "index_version1.compressed", "index_version1_docstats.txt");
        createStemIndexes(args[0], args[1], "index_version2.uncompress", "index_version2.compressed", "index_version2_docstats.txt");
    }

    private static void createLemmaIndexes(String corpusFilepath, String stopwordsFilepath, String uncompressedIndexFilename, String compressedIndexFilename, String statFilename) {
        System.out.println("Creating index version 1...");
        System.out.println("Tokenizing.. Timestamp: " + System.currentTimeMillis());
        Tokenizer tokenizer = new Tokenizer(Tokenizer.LEMMA_TOKENS);
        tokenizer.tokenize(corpusFilepath, tokenizer.getFilter(), true);
        Map<String, List<String>> tokenMap = tokenizer.getTokenMap();

        System.out.println("Creating uncompressed index.. Timestamp: " + System.currentTimeMillis());
        CompressionManager compressionManager = new CompressionManager();
        SPIMI spimi = new SPIMI();
        spimi.clearIndex(uncompressedIndexFilename);
        spimi.createIndex(tokenMap, uncompressedIndexFilename);
        spimi.writeDocMapAndStats(tokenMap, statFilename);
        System.out.println("Creating compressed index.. Timestamp: " + System.currentTimeMillis());
        compressionManager.compressWithBlockCompression(spimi.index, compressionManager.GAMMA_ENCODING, (short) 8);
        spimi.writeCompressedIndexToRandomAccessFile(compressedIndexFilename, compressionManager, (short) 8);
        System.out.println("Done. Timestamp: " + System.currentTimeMillis());
    }

    private static void createStemIndexes(String corpusFilepath, String stopwordsFilepath, String uncompressedIndexFilename, String compressedIndexFilename, String statFilename) {
        System.out.println("Creating index version 2...");
        System.out.println("Tokenizing.. Timestamp: " + System.currentTimeMillis());
        Tokenizer tokenizer = new Tokenizer(Tokenizer.STEM_TOKENS);
        tokenizer.tokenize(corpusFilepath, tokenizer.getFilter(), true);
        Map<String, List<String>> tokenMap = tokenizer.getTokenMap();

        System.out.println("Creating uncompressed index.. Timestamp: " + System.currentTimeMillis());
        CompressionManager compressionManager = new CompressionManager();
        SPIMI spimi = new SPIMI();
        spimi.clearIndex(uncompressedIndexFilename);
        spimi.createIndex(tokenMap, uncompressedIndexFilename);
        spimi.writeDocMapAndStats(tokenMap, statFilename);
        System.out.println("Creating compressed index.. Timestamp: " + System.currentTimeMillis());
        compressionManager.compressWithBlkCmprsnAndFrontEncoding(spimi.index, compressionManager.DELTA_ENCODING, (short) 8);
        spimi.writeCompressedIndexToRandomAccessFile(compressedIndexFilename, compressionManager, (short) 8);
        System.out.println("Done. Timestamp: " + System.currentTimeMillis());
    }

    private void clearIndex(String indexFileName) {
        File file = new File(indexFileName);
        if(file.isFile()) {
            file.delete();
        }
    }

    private void writeDocMapAndStats(Map<String, List<String>> tokenMap, String mapAndStatsFile) {
        try {
            File file = new File(mapAndStatsFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            if(file.isDirectory()) {
                System.out.println("Invalid file path provided. Kindly provide a valid file path!");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for(int counter = 1; counter < docIds.length; counter++) {
                writer.write(counter + " " + docIds[counter] + " " + tokenMap.get(docIds[counter]).size() + " " + maxDocTermFrequency[counter] + "\n");
                writer.flush();
            }
            writer.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sortIndex() {
        index = index.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));
    }

    private void writeIndexToFile(String outFile) {
        try {
            File file = new File(outFile);
            if (file.exists()) {
                file.createNewFile();
            }
            if(file.isDirectory()) {
                System.out.println("Invalid file path provided. Kindly provide a valid file path!");
            }
            maxDocTermFrequency = new int[docIds.length];
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            for(String term: index.keySet()) {
                writer.write(getIndexFileItem(term, index.get(term)));
                writer.flush();
            }
            writer.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void writeIndexToRandomAccessFile(String outFile) {
        RandomAccessFile randomAccessFile = null;
        try {
            maxDocTermFrequency = new int[docIds.length];
            randomAccessFile = new RandomAccessFile(outFile, "rw");
            for(String term: index.keySet()) {
                writeTermAndPostingToRandomAccessFile(randomAccessFile, term, index.get(term));
            }
            randomAccessFile.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private String getIndexFileItem(String term, LinkedList<PostingFileItem> postingList) {
        StringBuilder builder = new StringBuilder();
        builder.append(term);
        for(int counter = 0; counter < (maxTermLength - term.length()); counter++) {
            builder.append(' ');
        }
        builder.append(postingList.size());
        for(PostingFileItem postingFileItem: postingList) {
            builder.append(postingFileItem.getDocId())
                    .append(postingFileItem.getTermFrequency());
            if(maxDocTermFrequency[postingFileItem.getDocId()] < postingFileItem.getTermFrequency()) {
                maxDocTermFrequency[postingFileItem.getDocId()] = postingFileItem.getTermFrequency();
            }
        }
        return builder.append("\n").toString();
    }

    private void writeTermAndPostingToRandomAccessFile(RandomAccessFile randomAccessFile, String term, LinkedList<PostingFileItem> postingList) {
        try {
            randomAccessFile.write(term.getBytes());
            for(int counter = 0; counter < (maxTermLength - term.length()); counter++) {
                randomAccessFile.write(0);
            }
            randomAccessFile.writeShort(postingList.size());
            for(PostingFileItem postingFileItem: postingList) {
                randomAccessFile.writeInt(postingFileItem.getDocId());
                randomAccessFile.writeInt(postingFileItem.getTermFrequency());
                if(maxDocTermFrequency[postingFileItem.getDocId()] < postingFileItem.getTermFrequency()) {
                    maxDocTermFrequency[postingFileItem.getDocId()] = postingFileItem.getTermFrequency();
                }
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void writeCompressedIndexToRandomAccessFile(String filePath, CompressionManager manager, short blockSize) {
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
            randomAccessFile.write(manager.getTermString().getBytes());
            int counter = 0;
            int[] termPointers = manager.getTermPointers();
            for(LinkedList<PostingFileItem> postingFileItems: manager.getCompressedPostingsList()) {
                randomAccessFile.writeShort(postingFileItems.size());
                for(PostingFileItem postingFileItem: postingFileItems) {
                    randomAccessFile.write(postingFileItem.getCode().toByteArray());
                    randomAccessFile.writeInt(postingFileItem.getTermFrequency());
                }
                if(counter % blockSize == 0) {
                    randomAccessFile.writeInt(termPointers[counter / blockSize]);
                }
                counter++;
            }
            randomAccessFile.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void printIndexStats() {
        HashSet<String> termSet = new HashSet<>();
        termSet.add("reynold");
        termSet.add("nasa");
        termSet.add("prandtl");
        termSet.add("flow");
        termSet.add("pressure");
        termSet.add("boundary");
        termSet.add("shock");
        for(String term: index.keySet()) {
            if(termSet.contains(term)) {
                int tf = 0;
                for(PostingFileItem postingFileItem: index.get(term)) {
                    tf += postingFileItem.getTermFrequency();
                }
                System.out.println(term + " " + index.get(term).size() + " " + tf + " " + index.get(term).size() * Integer.BYTES);
            }
        }
    }
}
