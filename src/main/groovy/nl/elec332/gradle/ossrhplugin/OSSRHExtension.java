package nl.elec332.gradle.ossrhplugin;

import groovy.lang.Closure;
import org.gradle.api.Project;

import javax.inject.Inject;
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

    public void license(Closure<?> closure) {
        MavenConfigurator.configureLicense(project, closure);
    }

    public void developer(Closure<?> closure) {
        MavenConfigurator.configureDeveloper(project, closure);
    }

    public void contributor(Closure<?> closure) {
        MavenConfigurator.configureContributor(project, closure);
    }

    public void licenses(Closure<?> closure) {
        MavenConfigurator.configure(project, closure, "licenses");
    }

    public void developers(Closure<?> closure) {
        MavenConfigurator.configure(project, closure, "developers");
    }

    public void contributors(Closure<?> closure) {
        MavenConfigurator.configure(project, closure, "contributors");
    }

    public void issueManagement(Closure<?> closure) {
        MavenConfigurator.configure(project, closure, "issueManagement");
    }

}
