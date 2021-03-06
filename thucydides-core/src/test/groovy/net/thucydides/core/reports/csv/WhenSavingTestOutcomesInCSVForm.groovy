package net.thucydides.core.reports.csv

import au.com.bytecode.opencsv.CSVReader
import com.github.goldin.spock.extensions.tempdir.TempDir
import net.thucydides.core.model.TestOutcome
import net.thucydides.core.reports.TestOutcomeLoader
import net.thucydides.core.reports.TestOutcomes
import net.thucydides.core.reports.csv.CSVReporter
import net.thucydides.core.util.MockEnvironmentVariables
import spock.lang.Specification

import static net.thucydides.core.util.TestResources.directoryInClasspathCalled

/**
 * Test outcomes can be saved as CSV files, so they can be imported and manipulated in Excel.
 */
class WhenSavingTestOutcomesInCSVForm extends Specification {

    @TempDir File temporaryDirectory
    def loader = new TestOutcomeLoader()

    def "should store an empty set of test outcomes as an empty CSV file with only column titles"() {
        given: "no test results"
            def testOutcomeList = TestOutcomes.withNoResults()
        when: "we store these outcomes as a CSV file"
            def csvReporter = new CSVReporter(temporaryDirectory)
            File csvResults = csvReporter.generateReportFor(testOutcomeList, "results.csv")
        then: "the CSV file contains a single line"
            csvResults.text.readLines().size() == 1
        and: "the first line should contain the test outcome headings"
            linesIn(csvResults)[0] == ["Story", "Title", "Result", "Date", "Stability", "Duration (s)"]
    }

    def "should store a row of data for each test result"() {
        given: "a set of test results"
            def testOutcomeList = loader.loadFrom(directoryInClasspathCalled("/tagged-test-outcomes"));
        when: "we store these outcomes as a CSV file"
            def csvReporter = new CSVReporter(temporaryDirectory)
            File csvResults = csvReporter.generateReportFor(TestOutcomes.of(testOutcomeList), "results.csv")
        then: "there should be a row for each test result"
            def lines = linesIn(csvResults)
            lines.size() == 4
    }

    def environmentVariables = new MockEnvironmentVariables()

    def "should store user-configurable extra columns"() {
        given: "a set of test results"
            def loader = new TestOutcomeLoader()
            def testOutcomeList = loader.loadFrom(directoryInClasspathCalled("/tagged-test-outcomes"));
        and: "we want to store extra columns from tag values"
            environmentVariables.setProperty("thucydides.csv.extra.columns","feature, epic")
        when: "we store these outcomes as a CSV file"
            def csvReporter = new CSVReporter(temporaryDirectory, environmentVariables)
            File csvResults = csvReporter.generateReportFor(TestOutcomes.of(testOutcomeList), "results.csv")
        then: "the results should contain a column for each additional column"
            linesIn(csvResults)[0] == ["Story", "Title", "Result", "Date", "Stability", "Duration (s)", "Feature", "Epic"]
        and: "the extra column data should come from the tags"
           linesIn(csvResults)[1][6] == "" && linesIn(csvResults)[1][7] == "an epic" &&
           linesIn(csvResults)[2][6] == "A Feature" && linesIn(csvResults)[2][7] == ""
    }

    def linesIn(File csvResults) {
        def reader = new CSVReader(new FileReader(csvResults))
        reader.readAll()
    }
}
