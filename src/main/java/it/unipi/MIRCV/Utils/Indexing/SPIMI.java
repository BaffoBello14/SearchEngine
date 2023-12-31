package it.unipi.MIRCV.Utils.Indexing;

import it.unipi.MIRCV.Utils.PathAndFlags.PathAndFlags;
import it.unipi.MIRCV.Utils.Preprocessing.Preprocess;
import org.apache.commons.compress.archivers.*;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * SPIMI (Single Pass In-Memory Indexing) class for creating an inverted index from a collection.
 */
public class SPIMI {
    private static String PATH_TO_ARCHIVE_COLLECTION = "./Collection/collection.tar.gz";
    private static int numIndex = 0;
    private static long numPosting = 0;
    private static long offsetDocIndex = 0;
    private static int threshold = 80;

    /**
     * Sets the path to the archive collection.
     *
     * @param path The path to the archive collection.
     */
    public static void path_setter(String path) {
        PATH_TO_ARCHIVE_COLLECTION = path;
    }

    /**
     * Sets the threshold value.
     *
     * @param num The threshold value.
     */
    public static void threshold_setter(int num) {
        threshold = num;
    }

    /**
     * Executes the SPIMI algorithm to create an inverted index from the collection.
     *
     * @return The number of indexes created.
     * @throws Exception If an exception occurs during execution.
     */
    public static int execute() throws Exception {
        int doc_id = 1;
        String[] lineofDoc;
        String docno;
        FileChannel fileChannelDI = FileChannel.open(Paths.get(PathAndFlags.PATH_TO_DOC_INDEX), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

        int total_length = 0;
        List<String> tokens;
        Runtime runtime = Runtime.getRuntime();

        try {
            InputStream file = Files.newInputStream(Paths.get(PATH_TO_ARCHIVE_COLLECTION));
            InputStream gzip = new GZIPInputStream(file);
            ArchiveInputStream archiveStream = new ArchiveStreamFactory().createArchiveInputStream("tar", gzip);
            archiveStream.getNextEntry();
            BufferedReader reader = new BufferedReader(new InputStreamReader(archiveStream, StandardCharsets.UTF_8));
            String line;
            HashMap<String, PostingIndex> index = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                lineofDoc = Preprocess.parseLine(line);

                if (lineofDoc[1].trim().isEmpty()) {
                    continue;
                }

                docno = lineofDoc[0];
                lineofDoc[1] = Preprocess.cleanText(lineofDoc[1]);
                tokens = Preprocess.tokenize(lineofDoc[1]);

                if (PathAndFlags.STOPWORD_STEM_ENABLED) {
                    tokens = Preprocess.removeStopwords(tokens);
                    tokens = Preprocess.applyStemming(tokens);
                }

                if (tokens.isEmpty()) {
                    continue;
                }

                int documentLength = tokens.size();
                DocIndexEntry docIndexEntry = new DocIndexEntry(docno, documentLength);
                docIndexEntry.write2Disk(fileChannelDI, offsetDocIndex, doc_id);
                offsetDocIndex += DocIndexEntry.DOC_INDEX_ENTRY_SIZE;
                total_length += documentLength;

                for (String term : tokens) {
                    PostingIndex posting;
                    if (index.containsKey(term)) {
                        posting = index.get(term);
                    } else {
                        posting = new PostingIndex(term);
                        index.put(term, posting);
                    }
                    addPosting(doc_id, posting);
                }

                if (doc_id % 100000 == 0) {
                    System.out.println(doc_id + " processed");
                }
                doc_id++;

                if (((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory()) * 100 > threshold) {
                    if (!write2Disk(index)) {
                        System.out.println("Problems with writing to disk of SPIMI");
                        return -1;
                    }
                    index.clear();
                    System.gc();
                }
            }

            if (!index.isEmpty()) {
                if (!write2Disk(index)) {
                    System.out.println("Problems with writing to disk SPIMI");
                    return -1;
                }
            }

            index.clear();
            System.gc();
            file.close();
            reader.close();
            archiveStream.close();
            gzip.close();
        } catch (IOException | ArchiveException e) {
            e.printStackTrace();
            return -1;
        }

        CollectionStatistics.setTotalLenDoc(total_length);
        CollectionStatistics.setDocuments(doc_id - 1);
        CollectionStatistics.computeAVGDOCLEN();
        CollectionStatistics.write2Disk();
        fileChannelDI.close();

        return numIndex;
    }

    /**
     * Writes the inverted index to disk.
     *
     * @param index The inverted index to write.
     * @return True if writing is successful, false otherwise.
     */
    private static boolean write2Disk(HashMap<String, PostingIndex> index) {
        System.out.println("Save index with size -> " + index.size() + " the num -> " + numIndex);

        if (index.isEmpty()) {
            return true;
        }

        index = index.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        try {
            FileChannel fileChannelLex = FileChannel.open(Paths.get(PathAndFlags.PATH_TO_LEXICON + numIndex + ".dat"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            FileChannel fileChannelDOCID = FileChannel.open(Paths.get(PathAndFlags.PATH_TO_DOC_ID + numIndex + ".dat"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            FileChannel fileChannelFreq = FileChannel.open(Paths.get(PathAndFlags.PATH_TO_FREQ + numIndex + ".dat"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

            MappedByteBuffer mappedByteBufferDOCID = fileChannelDOCID.map(FileChannel.MapMode.READ_WRITE, 0, numPosting * 4L);
            MappedByteBuffer mappedByteBufferFreq = fileChannelFreq.map(FileChannel.MapMode.READ_WRITE, 0, numPosting * 4L);
            long lexOffset = 0;

            if (mappedByteBufferDOCID == null || mappedByteBufferFreq == null) {
                return false;
            }

            for (PostingIndex postingIndex : index.values()) {
                LexiconEntry lexiconEntry = new LexiconEntry();
                lexiconEntry.setOffset_doc_id(mappedByteBufferDOCID.position());
                lexiconEntry.setOffset_frequency(mappedByteBufferFreq.position());

                for (Posting posting : postingIndex.getPostings()) {
                    mappedByteBufferDOCID.putInt(posting.getDoc_id());
                    mappedByteBufferFreq.putInt(posting.getFrequency());
                }

                lexiconEntry.updateTFMAX(postingIndex);
                lexOffset = lexiconEntry.writeEntryToDisk(postingIndex.getTerm(), lexOffset, fileChannelLex);
            }

            numIndex++;
            numPosting = 0;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a posting to the inverted index.
     *
     * @param doc_id       The document ID.
     * @param postingIndex The posting index.
     */
    protected static void addPosting(int doc_id, PostingIndex postingIndex) {
        if (!postingIndex.getPostings().isEmpty()) {
            Posting posting = postingIndex.getPostings().get(postingIndex.getPostings().size() - 1);

            if (doc_id == posting.getDoc_id()) {
                posting.setFrequency(posting.getFrequency() + 1);
                return;
            }
        }

        postingIndex.getPostings().add(new Posting(doc_id, 1));
        numPosting++;
    }
}
