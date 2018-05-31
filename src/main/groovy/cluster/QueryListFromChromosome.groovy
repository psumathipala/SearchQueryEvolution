package cluster

import ec.vector.IntegerVectorIndividual
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.search.spans.SpanFirstQuery
import org.apache.lucene.search.spans.SpanTermQuery

@CompileStatic
class QueryListFromChromosome {

    static List<BooleanQuery.Builder> getORQueryList(int[] intChromosome, TermQuery[] termQueryArray, int numberOfClusters) {
        //list of boolean queries
        List<BooleanQuery.Builder> bqbL = []

        // set of genes - for duplicate checking
        Set<Integer> genes = [] as Set

        int arrayIndex=0
        for (int gene: intChromosome){
            final int clusterNumber = arrayIndex % numberOfClusters
            arrayIndex++

            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()
            if (gene < termQueryArray.size() && gene >= 0 && genes.add(gene)) {
                bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
            }
        }
        return bqbL
    }

    static Tuple2 getORQueryListSetK(int[] intArray, TermQuery[] termQueryArray) {
        //list of boolean queries
        List<BooleanQuery.Builder> bqbL = []

        // set of genes - for duplicate checking
        Set<Integer> genes = [] as Set

        int k = intArray[0]

        int gene
        //  intArray.eachWithIndex { int gene, int index ->
        for (int i = 1; i < intArray.size(); i++) {
            int clusterNumber = i % k
            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()
            gene = intArray[i]

            if (gene < termQueryArray.size() && gene >= 0 && bqbL[clusterNumber] && i <= (k * 2)) {
//} && genes.add(gene)) {
                //if (gene < termQueryArray.size() && gene >= 0   && genes.add(gene)) {
                bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
                //    bqbL[clusterNumber].setMinimumNumberShouldMatch(2)
            }
        }
        return new Tuple2(bqbL, k)
    }

    static List<BooleanQuery.Builder> getORDNFQueryListSetK(int[] intArray, TermQuery[] termQueryArray) {

        int k = intArray[0]
        int[] rest = intArray[1..intArray.size() - 1]

        return QueryListFromChromosome.getORDNFQueryList(rest, termQueryArray, k)
    }


    static List<BooleanQuery.Builder> getORDNFQueryList(int[] intArray, TermQuery[] termQueryArray, int numberOfClusters) {

        Set andPairSet = [] as Set
        TermQuery term0, term1
        int queryNumber = 0;

        List<BooleanQuery.Builder> bqbL = []

        intArray.eachWithIndex { int gene, int index ->

            if (index < numberOfClusters) {
                int clusterNumber = index % numberOfClusters
                bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

                if (gene < termQueryArray.size() && gene >= 0) {//&& genes.add(gene)) {
                    bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
                }

            } else if (gene < termQueryArray.size() && gene >= 0) {
                if (term0 == null) {
                    term0 = termQueryArray[gene]
                } else {
                    term1 = termQueryArray[gene]

                    //  Set andPair = [term0, term1] as Set
                    if (term0 != term1) {////&& andPairSet.add(andPair)) {

                        int clusterNumber = queryNumber % numberOfClusters
                        queryNumber++

                        BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
                        subbqb.add(term0, BooleanClause.Occur.MUST);
                        subbqb.add(term1, BooleanClause.Occur.MUST)
                        BooleanQuery subq = subbqb.build();

                        //check that the subquery returns something
                        TotalHitCountCollector collector = new TotalHitCountCollector();
                        Indexes.indexSearcher.search(subq, collector);
                        if (collector.getTotalHits() > 10) {
                            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()
                            bqbL[clusterNumber].add(subq, BooleanClause.Occur.SHOULD);
                        }
                    }
                    term0 = null;
                }
            }
        }
        return bqbL
    }


    static List<BooleanQuery.Builder> getORQueryListNot(int[] intArray, TermQuery[] termQueryArray, int numberOfClusters) {
        //list of boolean queries
        List<BooleanQuery.Builder> bqbL = []

        // set of genes - for duplicate checking
        Set<Integer> genes = [] as Set

        intArray.eachWithIndex { int gene, int index ->
            int clusterNumber = index % numberOfClusters
            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene < termQueryArray.size() && gene >= 0 && genes.add(gene)) {
                bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
            }
        }

        BooleanQuery qNot
        bqbL.eachWithIndex { BooleanQuery.Builder bqb, int index ->

            if (index == 0) {
                qNot = bqb.build()
            } else {
                BooleanQuery qn0 = bqb.build()
                def clauses = qNot.clauses()
                for (clause in clauses) {
                    bqb.add(clause.getQuery(), BooleanClause.Occur.MUST_NOT)
                }
                //   bqb.add(qNot, BooleanClause.Occur.MUST_NOT)
                qNot = qn0
            }
        }
        return bqbL
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public List getSpanFirstQL(IntegerVectorIndividual intVectorIndividual) {

        int spanValue, wordInd0
        //def word=null
        int qNumber = 0;
        def wordSet = [] as Set
        def duplicateCount = 0
        def sfMap = [0: 50, 1: 100, 2: 200, 3: 400, 4: 2000]

        def bqbList = []

        for (int i = 0; i < (intVectorIndividual.genome.length - 1); i = i + 2) {

            int clusterNumber = qNumber % Indexes.NUMBER_OF_CLUSTERS
            qNumber++
            bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

            if (intVectorIndividual.genome[i] >= termArray.length || intVectorIndividual.genome[i] < 0
                    || intVectorIndividual.genome[i + 1] >= termArray.length || intVectorIndividual.genome[i + 1] < 0
                    || intVectorIndividual.genome[i] == intVectorIndividual.genome[i + 1])
                continue;
            else {
                wordInd0 = intVectorIndividual.genome[i];
            }

            def term = termArray[wordInd0];

            if (!wordSet.add(term)) duplicateCount++

            def sfIndex = intVectorIndividual.genome[i + 1]
            def sfValue = sfMap[sfIndex]

            SpanFirstQuery sfq = new SpanFirstQuery(new SpanTermQuery(term),
                    sfValue);

            bqbList[clusterNumber].add(sfq, BooleanClause.Occur.SHOULD);
        }

        return [bqbList, duplicateCount]
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public List getORNOTfromEvolvedList(IntegerVectorIndividual intVectorIndividual) {

        def duplicateCount = 0
        def genes = [] as Set
        def bqbList = []
        int clusterNumber = -1

        intVectorIndividual.genome.eachWithIndex { gene, index ->
            def z = index % Indexes.NUMBER_OF_CLUSTERS
            if (z == 0) clusterNumber++
            //int clusterNumber =  0//index % IndexInfo.NUMBER_OF_CLUSTERS

            assert clusterNumber < Indexes.NUMBER_OF_CLUSTERS

            bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene >= 0) {

                //if (index >=  (intVectorIndividual.genome.size() -  IndexInfo.NUMBER_OF_CLUSTERS )){
                if (z == 4) {
                    //if ()
                    assert gene <= notWords20NG5.size()
                    //String wrd = notWords20NG5[gene]
                    TermQuery tq = new TermQuery(notWords20NG5[gene])
                    bqbList[clusterNumber].add(tq, BooleanClause.Occur.MUST_NOT)
                } else {
                    if (genes.add(gene) && gene < termArray.size()) {
                        //String wrd = termArray[gene]
                        TermQuery tq = new TermQuery(termArray[gene])
                        bqbList[clusterNumber].add(tq, BooleanClause.Occur.SHOULD)
                    }
                }
            }

        }
        return bqbList
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public List getORNOTQL(IntegerVectorIndividual intVectorIndividual) {

        def duplicateCount = 0
        def genes = [] as Set
        def bqbList = []

        intVectorIndividual.genome.eachWithIndex { gene, index ->

            int clusterNumber = index % Indexes.NUMBER_OF_CLUSTERS
            //String wrd = termArray[gene]
            bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene < termArray.size() && gene >= 0) {

                TermQuery tq = new TermQuery(termArray[gene])

                if (index >= (intVectorIndividual.genome.size() - Indexes.NUMBER_OF_CLUSTERS)) {
                    bqbList[clusterNumber].add(tq, BooleanClause.Occur.MUST_NOT)
                } else {
                    bqbList[clusterNumber].add(tq, BooleanClause.Occur.SHOULD)
                    if (!genes.add(gene)) {
                        duplicateCount = duplicateCount + 1
                    }
                }
            }
        }
        return [bqbList, duplicateCount]
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public List getALLNOTQL(IntegerVectorIndividual intVectorIndividual) {


        final MatchAllDocsQuery allQ = new MatchAllDocsQuery();

        //list of queries
        def bqbL = []
        // set of genes - for duplicate checking
        def genes = [] as Set

        //println "in allNot $allQ"

        intVectorIndividual.genome.eachWithIndex { gene, index ->
            int clusterNumber = index % Indexes.NUMBER_OF_CLUSTERS
            if (bqbL[clusterNumber] == null) {
                bqbL[clusterNumber] = new BooleanQuery.Builder()
                bqbL[clusterNumber].add(allQ, BooleanClause.Occur.SHOULD)
            }

            if (gene < termArray.size() && gene >= 0 && genes.add(gene)) {

                String word = termArray[gene]
                TermQuery tq = new TermQuery(new Term(Indexes.FIELD_CONTENTS, word))
                bqbL[clusterNumber].add(tq, BooleanClause.Occur.MUST_NOT)
            }
        }
        //println "end allNot bqbl  $bqbL"
        return bqbL
    }
}