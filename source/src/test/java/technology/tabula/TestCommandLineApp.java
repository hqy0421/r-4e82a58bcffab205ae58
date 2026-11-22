package technology.tabula;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestCommandLineApp {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String csvFromCommandLineArgs(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(CommandLineApp.buildOptions(), args);

        StringBuilder stringBuilder = new StringBuilder();
        new CommandLineApp(stringBuilder, cmd).extractTables(cmd);

        return stringBuilder.toString();
    }

    @Test
    public void testExtractSpreadsheetWithArea() throws ParseException, IOException {

        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/spreadsheet_no_bounding_frame.csv");

        assertEquals(expectedCsv, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/spreadsheet_no_bounding_frame.pdf",
                "-p", "1", "-a",
                "150.56,58.9,654.7,536.12", "-f",
                "CSV"
        }));
    }

    @Test
    public void testExtractBatchSpreadsheetWithArea() throws ParseException, IOException {
        FileSystem fs = FileSystems.getDefault();
        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/spreadsheet_no_bounding_frame.csv");
        Path tmpFolder = Files.createTempDirectory("tabula-java-batch-test");
        tmpFolder.toFile().deleteOnExit();

        Path copiedPDF = tmpFolder.resolve(fs.getPath("spreadsheet.pdf"));
        Path sourcePDF = fs.getPath("src/test/resources/technology/tabula/spreadsheet_no_bounding_frame.pdf");
        Files.copy(sourcePDF, copiedPDF);
        copiedPDF.toFile().deleteOnExit();

        this.csvFromCommandLineArgs(new String[]{
                "-b", tmpFolder.toString(),
                "-p", "1", "-a",
                "150.56,58.9,654.7,536.12", "-f",
                "CSV"
        });

        Path csvPath = tmpFolder.resolve(fs.getPath("spreadsheet.csv"));
        assertTrue(csvPath.toFile().exists());
        assertArrayEquals(expectedCsv.getBytes(), Files.readAllBytes(csvPath));
    }

    @Test
    public void testExtractSpreadsheetWithAreaAndNewFile() throws ParseException, IOException {

        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/spreadsheet_no_bounding_frame.csv");

        File newFile = folder.newFile();
        this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/spreadsheet_no_bounding_frame.pdf",
                "-p", "1", "-a",
                "150.56,58.9,654.7,536.12", "-f",
                "CSV", "-o", newFile.getAbsolutePath()
        });

        assertArrayEquals(expectedCsv.getBytes(), Files.readAllBytes(Paths.get(newFile.getAbsolutePath())));
    }


    @Test
    public void testExtractJSONWithArea() throws ParseException, IOException {

        String expectedJson = UtilsForTesting.loadJson("src/test/resources/technology/tabula/json/spanning_cells_basic.json");

        assertEquals(expectedJson, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/spanning_cells.pdf",
                "-p", "1", "-a",
                "150.56,58.9,654.7,536.12", "-f",
                "JSON"
        }));
    }

    @Test
    public void testExtractCSVWithArea() throws ParseException, IOException {

        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/spanning_cells.csv");

        assertEquals(expectedCsv, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/spanning_cells.pdf",
                "-p", "1", "-a",
                "150.56,58.9,654.7,536.12", "-f",
                "CSV"
        }));
    }

    @Test
    public void testGuessOption() throws ParseException, IOException {
        String expectedCsvNoGuessing = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/TestCommandLineApp_testGuessOption_no_guessing.csv");
        assertEquals(expectedCsvNoGuessing, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/icdar2013-dataset/competition-dataset-eu/eu-001.pdf",
                "-p", "1",
                "-f", "CSV"
        }));

        String expectedCsvWithGuessing = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/TestCommandLineApp_testGuessOption_with_guessing.csv");
        assertEquals(expectedCsvWithGuessing, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/icdar2013-dataset/competition-dataset-eu/eu-001.pdf",
                "-p", "1",
                "-f", "CSV",
                "-g"
        }));
    }

    @Test
    public void testEncryptedPasswordSupplied() throws ParseException {
        String s = this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/encrypted.pdf",
                "-s", "userpassword",
                "-p", "1",
                "-f", "CSV"
        });
        assertEquals("FLA Audit Profile,,,,,,,,,", s.split("\\r?\\n")[0]);
    }

    @Test(expected=org.apache.commons.cli.ParseException.class)
    public void testEncryptedWrongPassword() throws ParseException {
        String s = this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/encrypted.pdf",
                "-s", "wrongpassword",
                "-p", "1",
                "-f", "CSV"
        });
    }

    @Test
    public void testExtractWithMultiplePercentArea() throws ParseException, IOException {

        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/MultiColumn.csv");

        assertEquals(expectedCsv, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/MultiColumn.pdf",
                "-p", "1", "-a",
                "%0,0,100,50", "-a",
                "%0,50,100,100", "-f",
                "CSV"
        }));
    }

    @Test
    public void testExtractWithMultipleAbsoluteArea() throws ParseException, IOException {

        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/MultiColumn.csv");

        assertEquals(expectedCsv, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/MultiColumn.pdf",
                "-p", "1", "-a",
                "0,0,451,212", "-a",
                "0,212,451,425", "-f",
                "CSV"
        }));
    }

    @Test
    public void testExtractWithPercentAndAbsoluteArea() throws ParseException, IOException {

        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/MultiColumn.csv");

        assertEquals(expectedCsv, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/MultiColumn.pdf",
                "-p", "1", "-a",
                "%0,0,100,50", "-a",
                "0,212,451,425", "-f",
                "CSV"
        }));
    }

    @Test
    public void testLatticeModeWithColumnOption() throws ParseException, IOException {

        String expectedCsv = UtilsForTesting.loadCsv("src/test/resources/technology/tabula/csv/AnimalSounds.csv");

        assertEquals(expectedCsv, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/AnimalSounds.pdf",
                "-p", "1", "-c",
                "59,218,331,551",
                "-r", "-f", "CSV"
        }));
    }

    @Test
    public void testLatticeModeWithColumnAndMultipleAreasOption() throws ParseException, IOException {

        String expectedJson = UtilsForTesting.loadJson("src/test/resources/technology/tabula/json/AnimalSounds1.json");
        String resultJson = this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/AnimalSounds1.pdf",
                "-p", "1", "-c", "57,136,197,296,314,391,457,553",
                "-a", "%0,0,100,50", "-a", "%0,50,100,100",
                 "-r", "-f", "JSON"
        });
        assertEquals(expectedJson, resultJson);
    }

    private Path writeTemplate(String json) throws IOException {
        Path template = folder.newFile("column-template.json").toPath();
        Files.write(template, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return template;
    }

    @Test
    public void testNoTemplateKeepsOriginalBehavior() throws ParseException, IOException {
        // without a template the extraction path must be byte-identical to the
        // existing, expected CSV output
        String expectedCsv = UtilsForTesting.loadCsv(
                "src/test/resources/technology/tabula/csv/spanning_cells.csv");
        assertEquals(expectedCsv, this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/spanning_cells.pdf",
                "-p", "1", "-a",
                "150.56,58.9,654.7,536.12", "-f",
                "CSV"
        }));
    }

    @Test
    public void testColumnTemplatePlacesBlocksInAnchoredColumns() throws ParseException, IOException {
        // Argentina voting record data rows: name | bloque | provincia | voto.
        // Anchors are the measured left edges of the four columns (stream mode, no
        // rulings); -a restricts extraction to the data area below the page header.
        String json = "{"
                + "\"maxDeviation\": 6,"
                + "\"columns\": ["
                + "  {\"name\": \"name\",      \"anchor\": 28.56, \"align\": \"left\"},"
                + "  {\"name\": \"bloque\",    \"anchor\": 213.72, \"align\": \"left\"},"
                + "  {\"name\": \"provincia\", \"anchor\": 397.56, \"align\": \"left\"},"
                + "  {\"name\": \"voto\",      \"anchor\": 494.04, \"align\": \"left\"}"
                + "]}";
        Path template = writeTemplate(json);

        String csv = this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/argentina_diputados_voting_record.pdf",
                "-p", "1", "-t",
                "-a", "275,10,800,580",
                "-C", template.toString(),
                "-f", "CSV"
        });

        org.apache.commons.csv.CSVParser parser = org.apache.commons.csv.CSVParser.parse(
                new java.io.StringReader(csv),
                org.apache.commons.csv.CSVFormat.EXCEL);
        java.util.List<org.apache.commons.csv.CSVRecord> records = parser.getRecords();
        parser.close();
        assertTrue(records.size() > 10);

        for (org.apache.commons.csv.CSVRecord record : records) {
            // every emitted row keeps the full template width, including skipped columns
            assertEquals("row should keep 4 columns: " + record, 4, record.size());
        }

        // the first data row must land in its four anchored columns without shifting
        org.apache.commons.csv.CSVRecord firstDataRow = records.get(0);
        assertEquals("ABDALA de MATARAZZO, Norma Amanda", firstDataRow.get(0));
        assertEquals("Frente Cívico por Santiago", firstDataRow.get(1));
        assertEquals("Santiago del Estero", firstDataRow.get(2));
        assertEquals("AFIRMATIVO", firstDataRow.get(3));
    }

    @Test(expected = ParseException.class)
    public void testColumnTemplateConflictsWithLattice() throws ParseException, IOException {
        Path template = writeTemplate(
                "{\"maxDeviation\":2,\"columns\":[{\"anchor\":36,\"align\":\"left\"}]}");
        this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/argentina_diputados_voting_record.pdf",
                "-p", "1", "-l",
                "-C", template.toString()
        });
    }

    @Test(expected = ParseException.class)
    public void testColumnTemplateConflictsWithColumns() throws ParseException, IOException {
        Path template = writeTemplate(
                "{\"maxDeviation\":2,\"columns\":[{\"anchor\":36,\"align\":\"left\"}]}");
        this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/argentina_diputados_voting_record.pdf",
                "-p", "1", "-c", "59,218",
                "-C", template.toString()
        });
    }

    @Test(expected = ParseException.class)
    public void testMissingTemplateFileReported() throws ParseException {
        this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/argentina_diputados_voting_record.pdf",
                "-p", "1", "-t",
                "-C", "definitely-missing-template.json"
        });
    }

    @Test(expected = ParseException.class)
    public void testMalformedTemplateReported() throws ParseException, IOException {
        Path template = writeTemplate("{this is not valid json");
        this.csvFromCommandLineArgs(new String[]{
                "src/test/resources/technology/tabula/argentina_diputados_voting_record.pdf",
                "-p", "1", "-t",
                "-C", template.toString()
        });
    }

    @Test
    public void testAlignmentFailureIsReportedWithPageAndLine() throws ParseException, IOException {
        // anchors deliberately far away from every real block, so no legal
        // assignment exists: extraction must abort rather than silently emit
        // shifted columns
        Path template = writeTemplate("{"
                + "\"maxDeviation\": 1,"
                + "\"columns\": ["
                + "  {\"anchor\": 1000, \"align\": \"left\"},"
                + "  {\"anchor\": 1100, \"align\": \"left\"},"
                + "  {\"anchor\": 1200, \"align\": \"left\"},"
                + "  {\"anchor\": 1300, \"align\": \"left\"}"
                + "]}");
        try {
            this.csvFromCommandLineArgs(new String[]{
                    "src/test/resources/technology/tabula/argentina_diputados_voting_record.pdf",
                    "-p", "1", "-t",
                    "-a", "275,10,800,580",
                    "-C", template.toString()
            });
            fail("expected TemplateAlignmentException");
        } catch (technology.tabula.templates.TemplateAlignmentException e) {
            assertEquals(1, e.getPageNumber());
            assertTrue(e.getMessage().startsWith("page 1, line 1"));
        }
    }

}
