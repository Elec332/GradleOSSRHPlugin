package nl.elec332.gradle.ossrhplugin;

import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.MavenPlugin;
import org.gradle.api.tasks.bundling.Jar;
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
public class OSSRHPlugin implements Plugin<Project> {

    public static final String GITHUB_BASE_URL = "github.com";
    public static final String OSSRH_USERNAME_PROPERTY = "ossrh.username";
    public static final String OSSRH_PASSWORD_PROPERTY = "ossrh.password";

    @Inject
    public OSSRHPlugin(LocalMavenRepositoryLocator mavenRepositoryLocator) {
        this.mavenRepositoryLocator = mavenRepositoryLocator;
    }

    private final LocalMavenRepositoryLocator mavenRepositoryLocator;

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion("5.0");

        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(MavenPlugin.class);
        project.getPluginManager().apply(SigningPlugin.class);

        OSSRHExtension extension = project.getExtensions().create("ossrh", OSSRHExtension.class, project);

        Jar jDoc = project.getTasks().create("javadocJar", Jar.class);
        jDoc.setClassifier("javadoc");
        jDoc.from(JavaPluginHelper.getJavaDocTask(project));

        Jar sources = project.getTasks().create("sourcesJar", Jar.class);
        sources.setClassifier("sources");
        sources.from(JavaPluginHelper.getMainJavaSourceSet(project).getAllSource());

        GroovyHooks.addArtifact(project, jDoc);
        GroovyHooks.addArtifact(project, sources);

        SigningExtension signing = Objects.requireNonNull(project.getExtensions().getByType(SigningExtension.class));
        signing.sign(project.getConfigurations().getByName("archives"));

        project.afterEvaluate(a -> GroovyHooks.configureMaven(project, mavenDeployer -> {
            mavenDeployer.beforeDeployment(signing::signPom);
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
        }));

    }

    private String getLocalMavenProperty() {
        return Stream.of(
                System.getProperty("local_maven"), System.getenv("local_maven"),
                System.getProperty("MAVEN_LOCAL"), System.getenv("MAVEN_LOCAL")
        ).filter(Objects::nonNull).findFirst().orElse(null);
    }

}
