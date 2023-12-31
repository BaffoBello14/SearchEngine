package it.unipi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

import it.unipi.MIRCV.Converters.UnaryConverter;
import it.unipi.MIRCV.Converters.VariableByteEncoder;
import it.unipi.MIRCV.Utils.Indexing.*;
import org.junit.jupiter.api.Test;

import it.unipi.MIRCV.Utils.PathAndFlags.PathAndFlags;

/**
 * JUnit test class for Indexing.
 */
public class AIndexingTest {

    private static void createDirectoryIfNotExists(String directoryPath) {
        if (directoryPath != null && !directoryPath.isEmpty()) {
            try {
                if (!Files.exists(Paths.get(directoryPath))) {
                    Files.createDirectories(Paths.get(directoryPath));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void initialize() {
        // Directory paths
        PathAndFlags.PATH_TO_DOC_ID = "./IndexDataTest/Doc_ids/";
        PathAndFlags.PATH_TO_FINAL = "./IndexDataTest/Final";
        PathAndFlags.PATH_TO_FREQ = "./IndexDataTest/Freqs/";
        PathAndFlags.PATH_TO_LEXICON = "./IndexDataTest/Lexicons/";

        // File paths
        PathAndFlags.PATH_TO_FINAL_DOC_ID = "./IndexDataTest/Final/DocID.dat";
        PathAndFlags.PATH_TO_FINAL_FREQ = "./IndexDataTest/Final/Freq.dat";
        PathAndFlags.PATH_TO_DOC_INDEX = "./IndexDataTest/Doc_index/DocIndex.dat";
        PathAndFlags.PATH_TO_FINAL_LEXICON = "./IndexDataTest/Final/Lexicon.dat";
        PathAndFlags.PATH_TO_COLLECTION_STAT = "./IndexDataTest/CollectionStatistics/CollectionStat.dat";
        PathAndFlags.PATH_TO_BLOCK_FILE = "./IndexDataTest/BlockInfo/BlockInfo.dat";
        PathAndFlags.PATH_TO_FLAGS = "./IndexDataTest/Flags/Flags.dat";

        createDirectoryIfNotExists("./IndexDataTest/BlockInfo/");
        createDirectoryIfNotExists("./IndexDataTest/CollectionStatistics/");
        createDirectoryIfNotExists("./IndexDataTest/Doc_ids/");
        createDirectoryIfNotExists("./IndexDataTest/Doc_index/");
        createDirectoryIfNotExists("./IndexDataTest/Final/");
        createDirectoryIfNotExists("./IndexDataTest/Flags/");
        createDirectoryIfNotExists("./IndexDataTest/Freqs/");
        createDirectoryIfNotExists("./IndexDataTest/Lexicons/");
    }

    private static void testLexicon(String outputPath) {
        String FilePath = "./test_file/indexing_test.txt";

        try {
            FileChannel fc = FileChannel.open(Paths.get(outputPath),
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            BufferedReader reader = new BufferedReader(new FileReader(FilePath));
            String line;
            long offset = 0;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length >= 10) {
                    String term = fields[0].trim();
                    int offsetDocId = Integer.parseInt(fields[1].trim());
                    int upperTF = Integer.parseInt(fields[2].trim());
                    int df = Integer.parseInt(fields[3].trim());
                    float idf = Float.parseFloat(fields[4].trim());
                    float upperTFIDF = Float.parseFloat(fields[5].trim());
                    float upperBM25 = Float.parseFloat(fields[6].trim());
                    int offsetFrequency = Integer.parseInt(fields[7].trim());
                    int offsetSkipPointer = Integer.parseInt(fields[8].trim());
                    int numBlocks = Integer.parseInt(fields[9].trim());

                    LexiconEntry le = new LexiconEntry();
                    le.setTerm(term);
                    le.setOffset_doc_id(offsetDocId);
                    le.setUpperTF(upperTF);
                    le.setDf(df);
                    le.setIdf(idf);
                    le.setUpperTFIDF(upperTFIDF);
                    le.setUpperBM25(upperBM25);
                    le.setOffset_frequency(offsetFrequency);
                    le.setOffset_skip_pointer(offsetSkipPointer);
                    le.setNumBlocks(numBlocks);

                    offset = le.writeEntryToDisk(term, offset, fc);
                }
            }
            reader.close();
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testDodId(String outputPath) {
        String filePath = "./test_file/docId_test.txt";
        try {
            FileChannel fc = FileChannel.open(Paths.get(outputPath),
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            line = reader.readLine();
            String[] fields = line.split(",");
            MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fields.length);
            for (String num : fields) {
                byte[] doc_id = VariableByteEncoder.encode(Integer.parseInt(num));
                mappedByteBuffer.put(doc_id);
            }
            reader.close();
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testFreqs(String outputPath) {
        String filePath = "./test_file/freq_test.txt";
        try {
            FileChannel fc = FileChannel.open(Paths.get(outputPath),
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));

            String line;
            long offset = 0;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                int[] freq = new int[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    freq[i] = Integer.parseInt(fields[i]);
                }
                byte[] converted = UnaryConverter.convertToUnary(freq);
                MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, offset, converted.length);
                offset += converted.length;
                mappedByteBuffer.put(converted);
            }
            reader.close();
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testBlockInfo(String outputPath) {
        String FilePath = "./test_file/blockInfo_test.txt";

        try {
            FileChannel fc = FileChannel.open(Paths.get(outputPath),
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            BufferedReader reader = new BufferedReader(new FileReader(FilePath));
            String line;
            SkippingBlock.setFile_offset(0);

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                System.out.println(fields[0]);
                SkippingBlock skippingBlock;
                if (fields.length == 6) {
                    skippingBlock = new SkippingBlock();
                    skippingBlock.setDoc_id_offset(Long.parseLong(fields[0]));
                    skippingBlock.setDoc_id_size(Integer.parseInt(fields[1]));
                    skippingBlock.setFreq_offset(Long.parseLong(fields[2]));
                    skippingBlock.setFreq_size(Integer.parseInt(fields[3]));
                    skippingBlock.setDoc_id_max(Integer.parseInt(fields[4]));
                    skippingBlock.setNum_posting_of_block(Integer.parseInt(fields[5]));
                    skippingBlock.writeOnDisk(fc);
                }
            }
            reader.close();
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testDocIndex(String outputPath) {
        String FilePath = "./test_file/docIndex_test.txt";

        try {
            FileChannel fc = FileChannel.open(Paths.get(outputPath),
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            BufferedReader reader = new BufferedReader(new FileReader(FilePath));
            String line;
            long offset = 0;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                DocIndexEntry docIndexEntry;
                if (fields.length == 3) {
                    docIndexEntry = new DocIndexEntry(fields[1], Integer.parseInt(fields[2]));
                    offset = docIndexEntry.write2Disk(fc, offset, Integer.parseInt(fields[0]));
                }
            }
            reader.close();
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIndexesComparison() throws Exception {
        String lexiconGroundTruePath = "./IndexDataTest/Final/LexiconGroundTrue.dat";
        String lexiconPath = "./IndexDataTest/Final/Lexicon.dat";
        String docIdGroundTruePath = "./IndexDataTest/Final/DocIdGroundTrue.dat";
        String docIdpath = "./indexDataTest/Final/DocId.dat";
        String freqGroundTruePath = "./IndexDataTest/Final/FreqGroundTrue.dat";
        String freqpath = "./indexDataTest/Final/Freq.dat";
        String docIndexGroundTruePath = "./IndexDataTest/Doc_index/DocIndexGroundTrue.dat";
        String docIndexpath = "./indexDataTest/Doc_index/DocIndex.dat";
        String blockInfoGroundTruePath = "./IndexDataTest/BlockInfo/BlockInfoGroundTrue.dat";
        String blockInfopath = "./indexDataTest/BlockInfo/BlockInfo.dat";
        initialize();

        SPIMI.path_setter("./test_file/test_collection.tar.gz");
        SPIMI.threshold_setter(1);
        int spimi = SPIMI.execute();
        System.out.println(spimi);
        SPIMIMerger.setNumIndex(spimi);
        SPIMIMerger.execute();

        // Esegui il test per scrivere il file .dat
        testLexicon(lexiconGroundTruePath);
        testDodId(docIdGroundTruePath);
        testFreqs(freqGroundTruePath);
        testDocIndex(docIndexGroundTruePath);
        testBlockInfo(blockInfoGroundTruePath);

        // Confronta il file creato con il file di riferimento
        assertTrue(compareFiles(lexiconGroundTruePath, lexiconPath));
        assertTrue(compareFiles(docIdpath, docIdGroundTruePath));
        assertTrue(compareFiles(freqpath, freqGroundTruePath));
        assertTrue(compareFiles(docIndexGroundTruePath, docIndexpath));
        assertTrue(compareFiles(blockInfopath, blockInfoGroundTruePath));
    }

    public static boolean compareFiles(String filePath1, String filePath2) throws IOException {
        try (FileChannel fc1 = FileChannel.open(Paths.get(filePath1), StandardOpenOption.READ);
             FileChannel fc2 = FileChannel.open(Paths.get(filePath2), StandardOpenOption.READ)) {
            AtomicLong offset1 = new AtomicLong(0);
            AtomicLong offset2 = new AtomicLong(0);

            while (offset1.get() < fc1.size() && offset2.get() < fc2.size()) {
                MappedByteBuffer mp1 = fc1.map(FileChannel.MapMode.READ_ONLY, offset1.get(), 1);
                MappedByteBuffer mp2 = fc2.map(FileChannel.MapMode.READ_ONLY, offset2.get(), 1);

                byte byte1 = mp1.get();
                byte byte2 = mp2.get();

                if (byte1 != byte2) {
                    return false; // I file hanno elementi diversi
                }

                offset1.incrementAndGet();
                offset2.incrementAndGet();
            }

            // Se tutti i test sono passati e i due file hanno la stessa lunghezza e nessuna differenza
            return offset2.get() == fc2.size() || offset1.get() == fc1.size();
        }
    }
}
