package nl.elec332.gradle.ossrhplugin;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.publish.maven.*;

import javax.inject.Inject;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Elec332 on 18-4-2020
 */
public class OSSRHExtension {

    public OSSRHExtension(Project project) {
        this.project = project;
        this.snapshotFilter = version -> {
            if (version != null) {
                Matcher m = VERSION_FILE_PATTERN.matcher(version);
                if (m.matches()) {
                    project.setVersion(m.group(1) + "-" + SNAPSHOT_VERSION);
                    return true;
                } else {
                    return version.endsWith(SNAPSHOT_VERSION) || version.equals(LATEST_VERSION);
                }
            } else {
                return false;
            }
        };
        this.testFilter = version -> version.toLowerCase(Locale.ROOT).contains("-test") || version.contains("TEST");
    }

    public static final String LATEST_VERSION = "LATEST";
    public static final String SNAPSHOT_VERSION = "SNAPSHOT";
    public static final Pattern VERSION_FILE_PATTERN = Pattern.compile("^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$");

    private final Project project;

    @Inject
    public String repositoryUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/";

    @Inject
    public boolean useSnapshotRepo = false;

    @Inject
    public Predicate<String> snapshotFilter;

    @Inject
    public String snapshotRepositoryUrl = "https://oss.sonatype.org/content/repositories/snapshots/";

    @Inject
    public boolean testLocalRepo = false;

    @Inject
    public Predicate<String> testFilter;

    @Inject
    public String localRepository = null;

    @Inject
    public String name;

    @Inject
    public String artifactId;

    @Inject
    public String packaging = "jar";

    @Inject
    public String description;

    @Inject
    public String inceptionYear;

    @Inject
    public String url;

    @Inject
    public String githubUrl;

    @Inject
    public boolean useGithubIssues = true;

    @Inject
    public String scmUrl;

    @Inject
    public String scmConnection;

    @Inject
    public String scmDevConnection;

    public void license(Action<? super MavenPomLicense> closure) {
        licenses(spec -> spec.license(closure));
    }

    public void developer(Action<? super MavenPomDeveloper> closure) {
        developers(spec -> spec.developer(closure));
    }

    public void contributor(Action<? super MavenPomContributor> closure) {
        contributors(spec -> spec.contributor(closure));
    }

    public void licenses(Action<? super MavenPomLicenseSpec> closure) {
        MavenConfigurator.configure(project, pom -> pom.licenses(closure));
    }

    public void developers(Action<? super MavenPomDeveloperSpec> closure) {
        MavenConfigurator.configure(project, pom -> pom.developers(closure));
    }

    public void contributors(Action<? super MavenPomContributorSpec> closure) {
        MavenConfigurator.configure(project, pom -> pom.contributors(closure));
    }

    public void issueManagement(Action<? super MavenPomIssueManagement> closure) {
        MavenConfigurator.configure(project, pom -> pom.issueManagement(closure));
    }

}
