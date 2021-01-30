package nl.elec332.gradle.ossrhplugin;

import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.MavenHooks;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.JavaVersion;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;

import javax.inject.Inject;
import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 18-4-2020
 */
@NonNullApi
@SuppressWarnings("UnstableApiUsage")
public class OSSRHPlugin implements Plugin<Project> {

    public static final String GITHUB_BASE_URL = "github.com";
    public static final String OSSRH_USERNAME_PROPERTY = "ossrh.username";
    public static final String OSSRH_PASSWORD_PROPERTY = "ossrh.password";
    public static final String MAVEN_POM_CONFIGURATION = "mavenPom";

    @Inject
    public OSSRHPlugin(LocalMavenRepositoryLocator mavenRepositoryLocator) {
        this.mavenRepositoryLocator = mavenRepositoryLocator;
    }

    private final LocalMavenRepositoryLocator mavenRepositoryLocator;

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion("6.0");

        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(MavenPublishPlugin.class);
        project.getPluginManager().apply(SigningPlugin.class);

        OSSRHExtension extension = project.getExtensions().create("ossrh", OSSRHExtension.class, project);

        JavaPluginHelper.getJavaExtension(project).withSourcesJar();
        JavaPluginHelper.getJavaExtension(project).withJavadocJar();


        SigningExtension signing = project.getExtensions().getByType(SigningExtension.class);
        project.afterEvaluate(a -> MavenHooks.configureMaven(project, mavenDeployer -> {
            signing.sign(mavenDeployer);
            MavenConfigurator.configureMaven(mavenDeployer, extension, project, () -> {
                String ret = getLocalMavenProperty();
                if (!Utils.isNullOrEmpty(ret)) {
                    ret = "file://" + ret;
                } else {
                    ret = mavenRepositoryLocator.getLocalMavenRepository().getAbsolutePath();
                }
                if (!ret.endsWith(File.separator)) {
                    ret += File.separator;
                }
                return ret;
            });
            addMavenPomDeps(project, mavenDeployer);
        }));

        fixJavaDoc(project);
    }

    /**
     * @see org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
     * @see org.gradle.api.publish.maven.tasks.GenerateMavenPom
     */
    private static void addMavenPomDeps(Project project, MavenPublication publication) {
        Configuration conf = project.getConfigurations().create(MAVEN_POM_CONFIGURATION);
        project.afterEvaluate(p -> conf.getDependencies().forEach(d -> {
            if (d instanceof ModuleDependency) {
                MavenHooks.addDependency(publication, (ModuleDependency) d, "compile");
            }
        }));
    }

    /**
     * Due to a bug in javadoc in JDK versions up to JDK-11
     * dependencies that have their module-info file in a multi-version package
     * will cause a crash, because javadoc doesn't scan those locations
     *
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8208269">JDK bug report</a>
     */
    private static void fixJavaDoc(Project project) {
        if (JavaVersion.current().compareTo(JavaVersion.VERSION_11) <= 0) {
            project.getGradle().getTaskGraph().whenReady(tg ->
                    project.getTasks().withType(Javadoc.class).forEach(jDocTask ->
                            jDocTask.exclude("*module-info.java")
                    )
            );
        }
    }

    private String getLocalMavenProperty() {
        return Stream.of(
                System.getProperty("local_maven"), System.getenv("local_maven"),
                System.getProperty("MAVEN_LOCAL"), System.getenv("MAVEN_LOCAL")
        ).filter(Objects::nonNull).findFirst().orElse(null);
    }

}
