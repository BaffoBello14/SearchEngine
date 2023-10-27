package it.unipi.MIRCV.Utils.Indexing;

import it.unipi.MIRCV.Utils.PathAndFlags.PathAndFlags;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class CollectionStatistics {
    private static int documents;
    private static double avgDocLen;
    private static long totalLenDoc;
    protected static long ENTRY_SIZE = 4 + 8 + 8+8;
    private static long terms;
    public static int getDocuments() {
        return documents;
    }
    public static  void computeAVGDOCLEN(){
        avgDocLen= (double) totalLenDoc /documents;
    }

    public static void setDocuments(int documents1) {
        documents = documents1;
    }

    public static double getAvgDocLen() {
        return avgDocLen;
    }

    public static void setAvgDocLen(double avgDocLen1) {
        avgDocLen = avgDocLen1;
    }

    public static long getTerms() {
        return terms;
    }

    public static void setTerms(long terms1) {
        terms = terms1;
    }

    public static long getTotalLenDoc() {
        return totalLenDoc;
    }

    public static void setTotalLenDoc(long totalLenDoc) {
        CollectionStatistics.totalLenDoc = totalLenDoc;
    }

    public static boolean write2Disk(){
        try{
            FileChannel fileChannel= FileChannel.open(Paths.get(PathAndFlags.PATH_TO_COLLECTION_STAT), StandardOpenOption.READ,StandardOpenOption.WRITE,StandardOpenOption.CREATE);
            MappedByteBuffer mappedByteBuffer= fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, ENTRY_SIZE);
            mappedByteBuffer.putInt(documents);
            mappedByteBuffer.putDouble(avgDocLen);
            mappedByteBuffer.putLong(terms);
            mappedByteBuffer.putLong(totalLenDoc);
            fileChannel.close();
            return true;
        }catch (IOException e){
            System.out.println("Problems with writing to collection statistics file");
            return false;
        }

    }
    public static boolean readFromDisk() {
        try {
            FileChannel fileChannel=FileChannel.open(Paths.get(PathAndFlags.PATH_TO_COLLECTION_STAT),StandardOpenOption.READ);
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, ENTRY_SIZE);

            // Read data from the MappedByteBuffer
            documents = mappedByteBuffer.getInt();
            avgDocLen = mappedByteBuffer.getDouble();
            terms = mappedByteBuffer.getLong();
            totalLenDoc=mappedByteBuffer.getLong();
            // Close the FileChannel and the FileInputStream
            fileChannel.close();
            return true;
        } catch (IOException e) {
            System.out.println("Problems with reading from the collection statistics file");
            return false;
        }
    }

}
