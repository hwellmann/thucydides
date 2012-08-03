package net.thucydides.core.reports.html;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import net.thucydides.core.issues.IssueTracking;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format text for HTML reports.
 * In particular, this integrates JIRA links into the generated reports.
 */
public class Formatter {

    private final static String ISSUE_NUMBER_REGEXP = "#([A-Z][A-Z0-9-_]*)?-?\\d+";
    private final static Pattern shortIssueNumberPattern = Pattern.compile(ISSUE_NUMBER_REGEXP);
    private final static String FULL_ISSUE_NUMBER_REGEXP = "([A-Z][A-Z0-9-_]*)-\\d+";
    private final static Pattern fullIssueNumberPattern = Pattern.compile(FULL_ISSUE_NUMBER_REGEXP);
    private final static String ISSUE_LINK_FORMAT = "<a href=\"{0}\">{1}</a>";

    private final IssueTracking issueTracking;

    @Inject
    public Formatter(IssueTracking issueTracking) {
        this.issueTracking = issueTracking;
    }


    static class IssueExtractor {
        private String workingCopy;

        IssueExtractor(String initialValue) {
            this.workingCopy = initialValue;
        }


        public List<String> getShortenedIssues() {
            Matcher matcher = shortIssueNumberPattern.matcher(workingCopy);

            ArrayList<String> issues = Lists.newArrayList();
            while (matcher.find()) {
                String issue = matcher.group();
                issues.add(issue);
                workingCopy = workingCopy.replaceFirst(issue,"");
            }

            return issues;
        }

        public List<String> getFullIssues() {
            Matcher unhashedMatcher = fullIssueNumberPattern.matcher(workingCopy);

            ArrayList<String> issues = Lists.newArrayList();
            while (unhashedMatcher.find()) {
                String issue = unhashedMatcher.group();
                issues.add(issue);
                workingCopy = workingCopy.replaceFirst(issue,"");
            }

            return issues;
        }

    }

    public static List<String> issuesIn(final String value) {

        IssueExtractor extractor = new IssueExtractor(value);

        List<String> issuesWithHash = extractor.getShortenedIssues();
        List<String> allIssues = extractor.getFullIssues();
        allIssues.addAll(issuesWithHash);

        return allIssues;
    }

    public String addLinks(final String value) {
        if (issueTracking == null) {
            return value;
        }
        String formattedValue = value;
        if (issueTracking.getIssueTrackerUrl() != null) {
            formattedValue = insertFullIssueTrackingUrls(value);
        }
        if (issueTracking.getShortenedIssueTrackerUrl() != null) {
            formattedValue = insertShortenedIssueTrackingUrls(formattedValue);
        }
        return formattedValue;
    }

    private String insertShortenedIssueTrackingUrls(String value) {
        String formattedValue = value;
        String issueUrlFormat = issueTracking.getShortenedIssueTrackerUrl();
        List<String> issues = shortenedIssuesIn(value);
        for(String issue : issues) {
            String issueUrl = MessageFormat.format(issueUrlFormat, stripLeadingHashFrom(issue));
            String issueLink = MessageFormat.format(ISSUE_LINK_FORMAT, issueUrl, issue);
            formattedValue = formattedValue.replaceAll(issue, issueLink);
        }
        return formattedValue;
    }

    public static List<String> shortenedIssuesIn(String value) {
        IssueExtractor extractor = new IssueExtractor(value);
        return extractor.getShortenedIssues();
    }

    public static List<String> fullIssuesIn(String value) {
        IssueExtractor extractor = new IssueExtractor(value);
        return extractor.getFullIssues();
    }

    private String insertFullIssueTrackingUrls(String value) {
        String formattedValue = value;
        String issueUrlFormat = issueTracking.getIssueTrackerUrl();
        List<String> issues = fullIssuesIn(value);
        for(String issue : issues) {
            String issueUrl = MessageFormat.format(issueUrlFormat, issue);
            String issueLink = MessageFormat.format(ISSUE_LINK_FORMAT, issueUrl, issue);
            formattedValue = formattedValue.replaceAll(issue, issueLink);
        }
        return formattedValue;
    }

    private String stripLeadingHashFrom(final String issue) {
        return issue.substring(1);
    }
}
