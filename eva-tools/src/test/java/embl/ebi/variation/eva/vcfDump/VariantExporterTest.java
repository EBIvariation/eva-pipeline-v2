/*
 * Copyright 2015 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package embl.ebi.variation.eva.vcfdump;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import embl.ebi.variation.eva.vcfdump.cellbasewsclient.CellbaseWSClient;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencb.biodata.models.feature.Region;
import org.opencb.biodata.models.variant.*;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.opencga.lib.common.Config;
import org.opencb.opencga.storage.core.StorageManagerException;
import org.opencb.opencga.storage.core.StorageManagerFactory;
import org.opencb.opencga.storage.core.variant.VariantStorageManager;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBIterator;
import org.opencb.opencga.storage.core.variant.adaptors.VariantSourceDBAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by jmmut on 2015-10-29.
 *
 * @author Jose Miguel Mut Lopez &lt;jmmut@ebi.ac.uk&gt;
 */
public class VariantExporterTest {
    private static final String DB_NAME = "VariantExporterTest";

    private static final Logger logger = LoggerFactory.getLogger(VariantExporterTest.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private static CellbaseWSClient cellBaseClient;
    private static VariantStorageManager variantStorageManager;
    private static VariantDBAdaptor variantDBAdaptor;
    private static VariantSourceDBAdaptor variantSourceDBAdaptor;

    /**
     * Clears and populates the Mongo collection used during the tests.
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException, URISyntaxException, IllegalAccessException, ClassNotFoundException, InstantiationException, StorageManagerException {
        cleanDBs();
        fillDB();

        Config.setOpenCGAHome(System.getenv("OPENCGA_HOME") != null ? System.getenv("OPENCGA_HOME") : "/opt/opencga");
        cellBaseClient = new CellbaseWSClient("hsapiens");
        logger.info("using cellbase: " + cellBaseClient.getUrl() + " version " + cellBaseClient.getVersion());
        variantStorageManager = StorageManagerFactory.getVariantStorageManager();
        variantDBAdaptor = variantStorageManager.getDBAdaptor(DB_NAME, null);
        variantSourceDBAdaptor = variantDBAdaptor.getVariantSourceDBAdaptor();
    }

    /**
     * Clears and populates the Mongo collection used during the tests.
     *
     * @throws UnknownHostException
     */
    @AfterClass
    public static void tearDownClass() throws UnknownHostException {
        cleanDBs();
    }

    @Test
    public void getSources() {
        VariantExporter variantExporter = new VariantExporter(null, null, new QueryOptions());

        // one study
        String study7Id = "7";
        List<String> studies = Collections.singletonList(study7Id);
        Map<String, VariantSource> sources = variantExporter.getSources(variantSourceDBAdaptor, studies);
        assertEquals(1, sources.size());
        VariantSource file = sources.get(study7Id);
        assertEquals(study7Id, file.getStudyId());
        assertEquals("6", file.getFileId());

        // two studies
        String study8Id = "8";
        studies = Arrays.asList(study7Id, study8Id);
        sources = variantExporter.getSources(variantSourceDBAdaptor, studies);
        assertEquals(2, sources.size());
        file = sources.get(study7Id);
        assertEquals(study7Id, file.getStudyId());
        assertEquals("6", file.getFileId());
        assertEquals(2504, file.getSamples().size());
        file = sources.get(study8Id);
        assertEquals(study8Id, file.getStudyId());
        assertEquals("5", file.getFileId());
        assertEquals(2504, file.getSamples().size());

        // one study not in database
        studies = Arrays.asList("2");
        sources = variantExporter.getSources(variantSourceDBAdaptor, studies);
        assertEquals(0, sources.size());

        // empty study filter
        studies = new ArrayList<>();
        sources = variantExporter.getSources(variantSourceDBAdaptor, studies);
        assertEquals(0, sources.size());
    }

    @Test
    public void getVcfHeaders() throws IOException {
        VariantExporter variantExporter = new VariantExporter(null, null, new QueryOptions());
        String study7Id = "7";
        String study8Id = "8";
        List<String> studies = Arrays.asList(study7Id, study8Id);
        Map<String, VariantSource> sources = variantExporter.getSources(variantSourceDBAdaptor, studies);

        Map<String, VCFHeader> headers = variantExporter.getVcfHeaders(sources);
        VCFHeader header = headers.get(study7Id);
        assertEquals(2504, header.getSampleNamesInOrder().size());
        assertTrue(header.hasGenotypingData());
        header = headers.get(study8Id);
        assertEquals(2504, header.getSampleNamesInOrder().size());
        assertTrue(header.hasGenotypingData());
    }

    @Test
    public void testExportOneStudy() throws Exception {
        List<String> studies = Collections.singletonList("7");
        String region = "20:61000-69000";
        QueryOptions query = new QueryOptions();
        Map<String, List<VariantContext>> exportedVariants = exportAndCheck(query, studies, region);
        checkExportedVariants(query, studies, exportedVariants);
    }

    @Test
    public void testExportTwoStudies() throws Exception {
        List<String> studies = Arrays.asList("7", "8");
        String region = "20:61000-69000";
        // TODO Both studies have 219 variants in the region... check if we can choose some region with different number of variants
        QueryOptions query = new QueryOptions();
        Map<String, List<VariantContext>> exportedVariants = exportAndCheck(query, studies, region);
        checkExportedVariants(query, studies, exportedVariants);
    }

//    @Test
//    public void textExportWithFilter() {
//        QueryOptions queryOptions = new QueryOptions();
//        // TODO: this test should fail, is not exporting all variants
//        queryOptions.put(VariantDBAdaptor.ID, "rs544625796");
//        List<String> studies = Collections.singletonList("7");
//        String region = "20:61000-69000";
//        eMap<String, List<VariantContext>> exportedVariants = xportAndCheck(queryOptions, studies, region);
//        checkExportedVariants
//                // annot-ct=SO%3A0001583
//
//    }
//    @Test
//    public void testFilter() throws Exception {
//        List<String> files = Arrays.asList("5");
//        List<String> studies = Arrays.asList("7");
//        QueryOptions query = getQuery(files, studies);
//        String outputDir = "/tmp/";
//
//        // tell all variables to filter with
//        query.put(VariantDBAdaptor.REGION, "20:61000-69000");
//        query.put(VariantDBAdaptor.REFERENCE, "A");
//
//        VariantExporter variantExporter = new VariantExporter(cellBaseClient, variantDBAdaptor, query);
//        List<String> outputFiles = variantExporter.vcfExport(outputDir, variantSourceDBAdaptor);
//
//        ////////// checks
//
//        assertEquals(studies.size(), outputFiles.size());
//        assertEquals(0, variantExporter.getFailedVariants());   // test file should not have failed variants
//
//        VariantDBIterator iterator = variantDBAdaptor.iterator(query);
//        assertEqualLinesFilesAndDB(outputFiles, iterator);
//
//        for (String outputFile : outputFiles) {
//            assertVcfOrderedByCoordinate(outputFile);
//            boolean delete = new File(outputFile).delete();
//            assertTrue(delete);
//        }
//    }

    // TODO: move logic of check sources to controller or to method get sources
//    @Test
//    public void testMissingStudy() throws Exception {
//        List<String> studies = Arrays.asList("7", "9"); // study 9 doesn't exist
//        String region = "20:61000-69000";
//        QueryOptions query = new QueryOptions();
//        Map<String, List<VariantContext>> exportedVariants = exportAndCheck(query, studies, region);
//        // TODO: should fail?
//        //        thrown.expect(IllegalArgumentException.class);  // comment this line to see the actual exception, making the test fail
//        checkExportedVariants(query, Collections.singletonList("7"), exportedVariants);
//    }
//
//    @Test
//    public void testMissingStudy() throws Exception {
//        List<String> studies = Arrays.asList("7", "9"); // study 9 doesn't exist
//        QueryOptions query = new QueryOptions();
//        query.add(VariantDBAdaptor.STUDIES, studies);
//        String outputDir = "/tmp/";
//
//        VariantExporter variantExporter = new VariantExporter(cellBaseClient, variantDBAdaptor, query);
//
//        thrown.expect(IllegalArgumentException.class);  // comment this line to see the actual exception, making the test fail
//        variantExporter.vcfExport(outputDir, variantSourceDBAdaptor);
//    }


//
//    @Test
//    public void testFilter() throws Exception {
//        List<String> files = Arrays.asList("5");
//        List<String> studies = Arrays.asList("7");
//        QueryOptions query = getQuery(files, studies);
//        String outputDir = "/tmp/";
//
//        // tell all variables to filter with
//        query.put(VariantDBAdaptor.REGION, "20:61000-69000");
//        query.put(VariantDBAdaptor.REFERENCE, "A");
//
//        VariantExporter variantExporter = new VariantExporter(cellBaseClient, variantDBAdaptor, query);
//        List<String> outputFiles = variantExporter.vcfExport(outputDir, variantSourceDBAdaptor);
//
//        ////////// checks
//
//        assertEquals(studies.size(), outputFiles.size());
//        assertEquals(0, variantExporter.getFailedVariants());   // test file should not have failed variants
//
//        VariantDBIterator iterator = variantDBAdaptor.iterator(query);
//        assertEqualLinesFilesAndDB(outputFiles, iterator);
//
//        for (String outputFile : outputFiles) {
//            assertVcfOrderedByCoordinate(outputFile);
//            boolean delete = new File(outputFile).delete();
//            assertTrue(delete);
//        }
//    }


    @Test
    public void testMissingCellbase() throws Exception {
        String studyId = "studyId";
        final VariantSource variantSource = createTestVariantSource(studyId);
        VariantFactory factory = new VariantVcfFactory();

        List<String> studies = Collections.singletonList(studyId);

        // test multiallelic. these first conversions should NOT fail, as they doesn't need the src
        String multiallelicLine = "1\t1000\tid\tC\tA,T\t100\tPASS\t.\tGT\t0|0\t0|0\t0|1\t1|1\t1|2\t0|1";
        List<Variant> variants = factory.create(variantSource, multiallelicLine);
        assertEquals(2, variants.size());
        removeSrc(variants);    // <---- this is the key point of the test

        VariantExporter variantExporter = new VariantExporter(null, null, null);
        Map<String, VariantContext> variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(0), studies, null);

        List<String> alleles = Arrays.asList("C", "A", ".");
        assertEqualGenotypes(variants.get(0), variantContext.get(studyId), alleles);

        variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(1), studies, null);
        alleles = Arrays.asList("C", "T", ".");
        assertEqualGenotypes(variants.get(1), variantContext.get(studyId), alleles);


        // test multiallelic + indel
        String multiallelicIndelLine = "1\t1000\tid\tC\tCA,T\t100\tPASS\t.\tGT\t0|0\t0|0\t0|1\t1|1\t1|2\t0|1";
        variants = factory.create(variantSource, multiallelicIndelLine);
        assertEquals(2, variants.size());
        removeSrc(variants);    // <---- this is the key point of the test

        variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(1), studies, null);
        alleles = Arrays.asList("C", "T", ".");
        assertEqualGenotypes(variants.get(1), variantContext.get(studyId), alleles);

        // the next exception is the only one that should throw
        thrown.expect(IllegalArgumentException.class);
        variantExporter.convertBiodataVariantToVariantContext(variants.get(0), studies, null);

    }

    @Test
    public void testGetVariantContextFromVariant() throws Exception {
        String studyId = "studyId";
        final VariantSource variantSource = createTestVariantSource(studyId);
        VariantFactory factory = new VariantVcfFactory();

        List<String> studyIds = Collections.singletonList(studyId);

        // test multiallelic
        String multiallelicLine = "1\t1000\tid\tC\tA,T\t100\tPASS\t.\tGT\t0|0\t0|0\t0|1\t1|1\t1|2\t0|1";
        List<Variant> variants = factory.create(variantSource, multiallelicLine);
        assertEquals(2, variants.size());

        VariantExporter variantExporter = new VariantExporter(cellBaseClient, null, null);
        Map<String, VariantContext> variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(0), studyIds, null);

        List<String> alleles = Arrays.asList("C", "A", ".");
        assertEqualGenotypes(variants.get(0), variantContext.get(studyId), alleles);

        variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(1), studyIds, null);
        alleles = Arrays.asList("C", "T", ".");
        assertEqualGenotypes(variants.get(1), variantContext.get(studyId), alleles);


        // test indel
        String indelLine = "1\t1000\tid\tN\tNA\t100\tPASS\t.\tGT\t0|0\t0|0\t0|1\t1|1\t1|0\t0|1";
        variants = factory.create(variantSource, indelLine);

        variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(0), studyIds, null);
        alleles = Arrays.asList("N", "NA");
        assertEqualGenotypes(variants.get(0), variantContext.get(studyId), alleles);


        // test multiallelic + indel
        String multiallelicIndelLine = "1\t1000\tid\tN\tNA,T\t100\tPASS\t.\tGT\t0|0\t0|0\t0|1\t1|1\t1|2\t0|1";
        variants = factory.create(variantSource, multiallelicIndelLine);
        assertEquals(2, variants.size());

        variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(0), studyIds, null);
        alleles = Arrays.asList("N", "NA", ".");
        assertEqualGenotypes(variants.get(0), variantContext.get(studyId), alleles);

        variantContext = variantExporter.convertBiodataVariantToVariantContext(variants.get(1), studyIds, null);
        alleles = Arrays.asList("N", "T", ".");
        assertEqualGenotypes(variants.get(1), variantContext.get(studyId), alleles);

    }


    private Map<String, List<VariantContext>> exportAndCheck(QueryOptions query, List<String> studies, String region) {
        VariantExporter variantExporter = new VariantExporter(cellBaseClient, null, null);
        query.put(VariantDBAdaptor.STUDIES, studies);
        query.add(VariantDBAdaptor.REGION, region);

        VariantDBIterator iterator = variantDBAdaptor.iterator(query);
        Map<String, List<VariantContext>> exportedVariants = variantExporter.export(iterator, new Region(region), studies);
        assertEquals(0, variantExporter.getFailedVariants());
        return exportedVariants;
    }

    private void checkExportedVariants(QueryOptions query, List<String> studies, Map<String, List<VariantContext>> exportedVariants) {
        // checks
        assertEquals(studies.size(), exportedVariants.keySet().size());

        for (String study : studies) {
            compareExportedVariantsWithDatabaseOnes(query, exportedVariants, study);
        }
    }

    private void compareExportedVariantsWithDatabaseOnes(QueryOptions query, Map<String, List<VariantContext>> exportedVariants, String study) {
        VariantDBIterator iterator;
        query.put(VariantDBAdaptor.STUDIES, Collections.singletonList(study));
        long iteratorSize = 0;
        iterator = variantDBAdaptor.iterator(query);
        while (iterator.hasNext()) {
            Variant variant = iterator.next();
            assertTrue(variantInExportedVariantsCollection(variant, exportedVariants.get(study)));
            iteratorSize++;
        }
        int studyExportedVariants = exportedVariants.get(study).size();
        logger.info("{} variants exported for study {}", studyExportedVariants, study);
        assertEquals(iteratorSize, studyExportedVariants);
    }

    private VariantSource createTestVariantSource(String studyId) {
        final VariantSource variantSource = new VariantSource("name", "fileId", studyId, "studyName");
        List<String> samples = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            samples.add("s"+i);
        }
        variantSource.setSamples(samples);
        return variantSource;
    }

    private static void cleanDBs() throws UnknownHostException {
        logger.info("Cleaning test DBs ...");
        MongoClient mongoClient = new MongoClient("localhost");
        List<String> dbs = Arrays.asList(DB_NAME);
        for (String dbName : dbs) {
            DB db = mongoClient.getDB(dbName);
            db.dropDatabase();
        }
        mongoClient.close();
    }

    private static void fillDB() throws IOException, InterruptedException {
        String dump = VariantExporterTest.class.getResource("/dump/").getFile();
        logger.info("restoring DB from " + dump);
        Process exec = Runtime.getRuntime().exec("mongorestore " + dump);
        exec.waitFor();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
        while ((line = bufferedReader.readLine()) != null) {
            logger.info("mongorestore output:" + line);
        }
        bufferedReader.close();
        bufferedReader = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
        while ((line = bufferedReader.readLine()) != null) {
            logger.info("mongorestore errorOutput:" + line);
        }
        bufferedReader.close();

        logger.info("mongorestore exit value: " + exec.exitValue());
    }

    private void removeSrc(List<Variant> variants) {
        for (Variant variant : variants) {
            for (VariantSourceEntry variantSourceEntry : variant.getSourceEntries().values()) {
                variantSourceEntry.getAttributes().remove("src");
            }
        }
    }
//
//    private void assertEqualLinesFilesAndDB(List<String> fileNames, VariantDBIterator iterator) throws IOException {
//        long lines = 0;
//        for (String fileName : fileNames) {
//            lines += countLines(fileName);
//        }
//
//        // counting variants in the DB
//        long variantRows = countRows(iterator);
//
//        assertEquals(variantRows, lines);
//    }
//
//    private long countRows(Iterator<Variant> iterator) {
//        int variantRows = 0;
//        while(iterator.hasNext()) {
//            iterator.next();
//            variantRows++;
//        }
//        return variantRows;
//    }
//
//    private long countLines(String fileName) throws IOException {
//        long lines;
//        lines = 0;
//        BufferedReader file = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
//        String line;
//        while ((line = file.readLine()) != null) {
//            if (line.charAt(0) != '#') {
//                lines++;
//            }
//        }
//        file.close();
//        return lines;
//    }
//
//    private void assertVcfOrderedByCoordinate(String fileName) {
//        logger.info("Checking that {} is sorted by coordinate", fileName);
//        Set<String> finishedContigs = new HashSet<>();
//        VCFFileReader vcfReader = new VCFFileReader(new File(fileName), false);
//        String lastContig = null;
//        int previousStart = -1;
//
//        for (VariantContext variant : vcfReader) {
//            // check chromosome
//            if (lastContig == null || !variant.getContig().equals(lastContig)) {
//                if (lastContig != null) {
//                    finishedContigs.add(lastContig);
//                }
//                lastContig = variant.getContig();
//                assertFalse("The variants should by grouped by contig in the vcf output", finishedContigs.contains(lastContig));
//                previousStart = -1;
//            }
//            assertTrue("The vcf is not sorted by coordinate: " + variant, variant.getStart() >= previousStart);
//            previousStart = variant.getStart();
//        }
//
//    }

    private void assertEqualGenotypes(Variant variant, VariantContext variantContext, List<String> alleles) {
        for (Map.Entry<String, Map<String, String>> data : variant.getSourceEntries().values().iterator().next().getSamplesData().entrySet()) {
            Genotype genotype = variantContext.getGenotype(data.getKey());
            String gt = data.getValue().get("GT");
            org.opencb.biodata.models.feature.Genotype biodataGenotype = new org.opencb.biodata.models.feature.Genotype(gt, alleles.get(0), alleles.get(1));
            assertEquals(Allele.create(alleles.get(biodataGenotype.getAllele(0)), biodataGenotype.isAlleleRef(0)),
                    genotype.getAllele(0));
            assertEquals(Allele.create(alleles.get(biodataGenotype.getAllele(1)), biodataGenotype.isAlleleRef(1)),
                    genotype.getAllele(1));
        }
    }
//
//    private QueryOptions getQuery(List<String> studies) {
//        QueryOptions query = new QueryOptions();
//        query.put(VariantDBAdaptor.STUDIES, studies);
//        return query;
//    }


    private static boolean variantInExportedVariantsCollection(Variant variant, List<VariantContext> exportedVariants) {
        if (exportedVariants.stream().anyMatch(v -> sameVariant(variant, v))) {
            return true;
        }

        return false;
    }

    private static boolean sameVariant(Variant v1, VariantContext v2) {
        if (v2.getContig().equals(v1.getChromosome()) && v2.getStart() == v1.getStart()) {
            if (v1.getReference().equals("")) {
                // insertion
                return v2.getAlternateAlleles().contains(Allele.create(v2.getReference().getBaseString() + v1.getAlternate()));
            } else if (v1.getAlternate().equals("")) {
                // deletion
                return v2.getAlternateAlleles().stream().anyMatch(alt -> v2.getReference().getBaseString().equals(alt.getBaseString() + v1.getReference()));
            } else {
                return v1.getReference().equals(v2.getReference().getBaseString()) && v2.getAlternateAlleles().contains(Allele.create(v1.getAlternate()));
            }
        }
        return false;
    }
}
