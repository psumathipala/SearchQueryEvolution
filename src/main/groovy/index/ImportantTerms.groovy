package index

import org.apache.lucene.document.Document
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.TFIDFSimilarity
import org.apache.lucene.util.BytesRef

/**
 * Return a list of termqueries likely to be useful for building boolean queries for classification or clustering
 * Terms should be in order of their likely usefulness in query building 
 * @author Laurie
 */

//the method for term selection / dimension reduction
enum ImportantTermsMethod {
    F1, TFIDF, IG, CHI, OR, MERGED
}

//@groovy.transform.CompileStatic
//@groovy.transform.TypeChecked
public class ImportantTerms {

    public final static int SPAN_FIRST_MAX_END = 300;
    private final static int MAX_TERMQUERYLIST_SIZE = 300;

    private final IndexSearcher indexSearcher = Indexes.indexSearcher;
    private final IndexReader indexReader = indexSearcher.indexReader
    private TermsEnum termsEnum
    private Set<String> stopSet = StopSet.getStopSetFromFile()

    public static final ImportantTermsMethod itm = ImportantTermsMethod.F1

    public static void main(String[] args) {
        Indexes.instance.setIndex(Indexes.indexEnum = IndexEnum.NG3)
        Indexes.instance.categoryNumber = '2'
        Indexes.instance.setIndexFieldsAndTotals()

        def iw = new ImportantTerms()
        //  iw.mergeMethods()
        // iw.getF1TermQueryList()
        //  iw.getTFIDFTermQueryList()
        //        iw.getTFIDFTermQueryListForCategory()
        //iw.getIGTermQueryList()
        //iw.getChiTermQueryList()
        // iw.getORTermQueryList()
    }

    public ImportantTerms() {

        Terms terms = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS)
        termsEnum = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS).iterator()

        println "Important words terms.getDocCount: ${terms.getDocCount()}"
        println "Important words terms.size ${terms.size()}"
        //println "FS method is" $itm
    }

    public TermQuery[] getImportantTerms() {


        switch (itm) {
            case itm.F1: return new FSmethods().getF1TermQueryList(); break;
            case itm.TFIDF: return new FSmethods().getTFIDFTermQueryList(); break;
            case itm.IG: return new FSmethods().getIGTermQueryList(); break;
            case itm.OR: return new FSmethods().getORTermQueryList(); break;
            case itm.MERGED: return new FSmethods().mergeMethods(); break;
            default: println "Incorrect selection method in getImportantTerms()";
        }

        //return getF1TermQueryList()
    }
}