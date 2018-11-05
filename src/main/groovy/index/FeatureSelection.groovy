package index

import org.apache.lucene.search.TermQuery

interface FeatureSelection {
    TermQuery[] getF1TermQueryList()
    TermQuery[] getTFIDFTermQueryList()
    //TermQuery[] getTFIDFTermQueryListForCategory()
    TermQuery[] getIGTermQueryList()
    TermQuery[] getORTermQueryList()
    TermQuery[] getChiTermQueryList()
    TermQuery[] mergeMethods()
}