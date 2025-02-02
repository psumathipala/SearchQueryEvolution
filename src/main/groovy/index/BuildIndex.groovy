package index

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader

import java.nio.file.Path
import java.nio.file.Paths

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

enum IndexName {
    R10, NG20, OHS,NG5, NG6
}

class BuildIndex {

    static main(args) {
        new BuildIndex()
    }

    BuildIndex() {

        IndexName iName = IndexName.NG6

        final String ohsIndexPath = 'indexes/Ohsc06MuscC08RespC11Eye'
        final String ohsDocsPath =/C:\Users\aceslh\Dataset\Ohsc06MuscC08RespC11Eye/

        final String r10DocsPath = // /C:\Users\Laurie\Dataset\R8/

        /C:\Users\aceslh\Dataset\r5/

        final String NG6path = /C:\Users\aceslh\Dataset\r5corn/
        // /C:\Users\Laurie\Dataset\reuters-top10/

        final String NGDocsPath =
         //      /C:\Users\Laurie\Dataset\20NG5WindowsMotorcyclesSpaceMedMideast/
              // /C:\Users\aceslh\Dataset\20NG5WindowsmiscForsaleHockeySpaceChristian/

                /C:\Users\aceslh\Dataset\r4Ship/
    //    /C:\Users\aceslh\Dataset\20NG6GraphicsHockeyCryptSpaceChristianGuns/
     //   /C:\Users\Laurie\Dataset\20NG3SpaceHockeyChristian/
               //   /C:\Users\Laurie\Dataset\20bydate/
        final String r10IndexPath = 'indexes/R4Ship'//'indexes/R10'
   //     final String NG20IndexPath = 'indexes/NG20SpaceHockeyChristianV7'
        final String NGIndexPath =
                //'indexes/20NG'
                //'indexes/20NG6GraphicsHockeyCryptSpaceChristianGuns'

        'indexes/R4Ship'



             //   'indexes/20NG5WindowsForsaleSpaceHockeyChristian'
        //        'indexes/20NG3SpaceHockeyChristian'
          //      'indexes/20NG5WindowsMotorcyclesSpaceMedMideast'

        String docsPath, indexPath

        if (iName == IndexName.R10) {
            docsPath = r10DocsPath
            indexPath = r10IndexPath
        } else if (iName == IndexName.NG6) {
            docsPath = NGDocsPath
            indexPath = NGIndexPath
        }
        else if (iName==IndexName.OHS){
            docsPath = ohsDocsPath
            indexPath = ohsIndexPath
        }

//Note: R10 - different directory structure
        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = //new EnglishAnalyzer();  //with stemming
                new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

//store doc counts for each category
        def catsNameFreq = [:]

// Create a new index in the directory, removing any
// previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE)
        IndexWriter writer = new IndexWriter(directory, iwc)
      //  IndexSearcher indexSearcher = new IndexSearcher(writer.getReader())
        Date start = new Date();
        println("Indexing to directory: $indexPath  from: $docsPath ...")

        def categoryNumber = -1

        new File(docsPath).eachDir {
            if (iName == IndexName.R10) categoryNumber++
            else categoryNumber = -1  //reset for 20NG for test and train directories

            int docCount=0
            it.eachFileRecurse { file ->
                if (iName != IndexName.R10 && file.isDirectory()) categoryNumber++
                if (!file.hidden && file.exists() && file.canRead() && !file.isDirectory() && docCount <100) // && categoryNumber <3)

                {
                    def doc = new Document()

                    Field catNumberField = new StringField(Indexes.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES);
                    doc.add(catNumberField)

                    Field pathField = new StringField(Indexes.FIELD_PATH, file.getPath(), Field.Store.YES);
                    doc.add(pathField);

                    String parent = file.getParent()
                    String grandParent = file.getParentFile().getParent()

                    def catName
                    //reuters dataset has different directory structure

                    if (iName == IndexName.R10)
                        catName = grandParent.substring(grandParent.lastIndexOf(File.separator) + 1, grandParent.length())
                    else
                        catName = parent.substring(parent.lastIndexOf(File.separator) + 1, parent.length())

                    Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName, Field.Store.YES);
                    doc.add(catNameField)

                    String test_train
                    if (file.canonicalPath.contains("test")) test_train = "test" else test_train = "train"
                 //   println "cannonicla ptath is" + file.canonicalPath
                //    println "test train $test_train"
                 //   println ""
                    Field ttField = new StringField(Indexes.FIELD_TEST_TRAIN, test_train, Field.Store.YES)
                    doc.add(ttField)

                    doc.add(new TextField(Indexes.FIELD_CONTENTS, file.text, Field.Store.YES))

                    def n = catsNameFreq.get((catName)) ?: 0
                    catsNameFreq.put((catName), n + 1)


                    writer.addDocument(doc)
                }
                docCount++
            }
        }
        println "Total docs: " + writer.maxDoc()
        writer.close()
        IndexReader indexReader = DirectoryReader.open(directory)
        IndexSearcher indexSearcher = new IndexSearcher(indexReader)
        TotalHitCountCollector trainCollector = new TotalHitCountCollector();
        final TermQuery trainQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "train"))

        TotalHitCountCollector testCollector = new TotalHitCountCollector();
        final TermQuery testQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "test"))

        indexSearcher.search(trainQ, trainCollector);
        def trainTotal = trainCollector.getTotalHits();

        indexSearcher.search(testQ, testCollector);
        def testTotal = testCollector.getTotalHits();

        Date end = new Date();
        println(end.getTime() - start.getTime() + " total milliseconds");
        println "testTotal $testTotal trainTotal $trainTotal"
        println "catsNameFreq $catsNameFreq"


        println "End ***************************************************************"
    }
}