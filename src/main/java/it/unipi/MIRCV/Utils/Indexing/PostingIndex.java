package it.unipi.MIRCV.Utils.Indexing;

import java.util.ArrayList;
import java.util.Iterator;

public class PostingIndex {
    private String term;
    private ArrayList<Posting>postings=new ArrayList<>();
    private ArrayList<SkippingBlock>blocks;
    private SkippingBlock skippingBlockActual;
    private Posting postingActual;
    private Iterator<Posting> postingIterator;
   // private int doc_len_BM25=1;
    //private int tf_BM25=0;
    private Iterator<SkippingBlock> skippingBlockIterator;
    public void closeLists(){
        postings.clear();
        blocks.clear();

    }
/*
    public void updateBM25Values(int tf, int doc_len){
        float ratio=(float)this.tf_BM25/(float)(this.doc_len_BM25+this.tf_BM25);
        float newRatio=(float)tf/(float)(doc_len+tf);
        if(newRatio>ratio){
            this.tf_BM25=tf;
            this.doc_len_BM25=doc_len;
        }
    }

    public int getDoc_len_BM25() {
        return doc_len_BM25;
    }

    public int getTf_BM25() {
        return tf_BM25;
    }
*/
    public PostingIndex(){}
    public PostingIndex(String term){
        this.term=term;
    }

    public Posting getPostingActual() {
        return postingActual;
    }

    public String getTerm() {
        return term;
    }

    public ArrayList<Posting> getPostings() {
        return postings;
    }

    public void setTerm(String term) {
        this.term = term;
    }
    public void addPostings(ArrayList<Posting> postings2Add){
        postings.addAll(postings2Add);
    }
    public void openList(){
        //need to read blocks form lexicon
        //blocks=get blocks from lexicon
        skippingBlockIterator= blocks.iterator();
        postingIterator=postings.iterator();
    }
    public Posting next(){
        if(!postingIterator.hasNext()){
            if(!skippingBlockIterator.hasNext()){
                postingActual=null;
                return null;

            }
            skippingBlockActual=skippingBlockIterator.next();

            postings.clear();
            postings.addAll(skippingBlockActual.getSkippingBlockPostings());
            postingIterator=postings.iterator();
        }
        postingActual=postingIterator.next();
        return postingActual;
    }
}
