package it.unipi.MIRCV.Utils.Indexing;

import it.unipi.MIRCV.Utils.PathAndFlags.PathAndFlags;

import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.io.*;
import java.nio.channels.FileChannel;

public class Lexicon {

    // Main data structure to hold lexicon entries
    private HashMap<String, LexiconEntry> lexicon = new HashMap<>();

    // Maximum length allowed for a term
    protected static final int MAX_LEN_OF_TERM = 32;

    // Getter method to retrieve the entire lexicon
    public HashMap<String, LexiconEntry> getLexicon() {
        return lexicon;
    }

    // Retrieves a lexicon entry based on the term. If the term is not in the in-memory lexicon, it tries to find it on disk.
    public LexiconEntry retrieveEntry(String term) {
        if (lexicon.containsKey(term)) {
            return lexicon.get(term);
        }
        LexiconEntry lexiconEntry = find(term);
        if (lexiconEntry == null) {
            return null;
        }
        add(term, lexiconEntry);
        return lexiconEntry;
    }

    // Attempts to find a term in the lexicon file on disk using binary search
    public LexiconEntry find(String term) {
        try {
            long top = CollectionStatistics.getTerms() - 1;
            long bot = 0;
            long mid;
            LexiconEntry entry = new LexiconEntry();

            try (FileChannel fileChannel = FileChannel.open(Paths.get(PathAndFlags.PATH_TO_LEXICON), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {

                while (bot <= top) {
                    mid = (bot + top) / 2;
                    entry.readEntryFromDisk(mid * LexiconEntry.ENTRY_SIZE, fileChannel);

                    String termFound = Lexicon.removePadding(entry.getTerm());
                    int comparisonResult = term.compareTo(termFound);

                    if (comparisonResult == 0) {
                        return entry;
                    } else if (comparisonResult > 0) {
                        bot = mid + 1;
                    } else {
                        top = mid - 1;
                    }
                }
            }
            return null;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Setter method to update the lexicon
    public void setLexicon(HashMap<String, LexiconEntry> lexicon) {
        this.lexicon = lexicon;
    }

    // Adds an entry to the lexicon
    public void add(String term, LexiconEntry lexiconEntry) {
        lexicon.put(term, lexiconEntry);
    }

    // Adds or updates a term in the lexicon
    public void add(String term) {
        lexicon.compute(term, (key, entry) -> {
            if (entry == null) {
                return new LexiconEntry();
            } else {
                entry.incrementDf();
                return entry;
            }
        });
    }

    // Returns a sorted list of terms in the lexicon
    public ArrayList<String> sortLexicon() {
        ArrayList<String> sorted = new ArrayList<>(lexicon.keySet());
        Collections.sort(sorted);
        return sorted;
    }

    // Pads a string to a specified length
    public static String padStringToLength(String input) {
        if (input.length() >= MAX_LEN_OF_TERM) {
            return input.substring(0, MAX_LEN_OF_TERM);
        } else {
            return String.format("%1$-" + MAX_LEN_OF_TERM + "s", input);
        }
    }

    // Removes any padding from a string
    public static String removePadding(String paddedString) {
        String trimmed = paddedString.trim();
        int nullIndex = trimmed.indexOf(' ');
        return nullIndex >= 0 ? trimmed.substring(0, nullIndex) : trimmed;
    }

    // Returns the IDF value for a given term
    public float getIDF(String term) {
        return lexicon.get(term).getIdf();
    }
}
