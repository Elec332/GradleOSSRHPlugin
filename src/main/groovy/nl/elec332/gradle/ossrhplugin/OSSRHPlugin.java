package nl.elec332.gradle.ossrhplugin;

import nl.elec332.gradle.util.*;
import org.gradle.api.JavaVersion;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.MavenPlugin;
import org.gradle.api.plugins.MavenPluginConvention;
import org.gradle.api.tasks.bundling.Jar;
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
        PluginHelper.checkMinimumGradleVersion("5.0");

        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(MavenPlugin.class);
        project.getPluginManager().apply(SigningPlugin.class);

        OSSRHExtension extension = project.getExtensions().create("ossrh", OSSRHExtension.class, project);

        Jar jDoc = project.getTasks().create("javadocJar", Jar.class);
        jDoc.setClassifier("javadoc");
        jDoc.from(JavaPluginHelper.getJavaDocTask(project));
        fixJavaDoc(project);

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

        addMavenPomDeps(project, extension);
    }

    private static void addMavenPomDeps(Project project, OSSRHExtension extension) {
        Configuration conf = project.getConfigurations().create(MAVEN_POM_CONFIGURATION);
        project.getPlugins().withType(MavenPlugin.class, plugin -> {
            Object conv = project.getConvention().getPlugins().get("maven");
            if (conv instanceof MavenPluginConvention) {
                ((MavenPluginConvention) conv).getConf2ScopeMappings().addMapping(MavenPlugin.COMPILE_PRIORITY, conf, Conf2ScopeMappingContainer.COMPILE);
            }
        });
        ProjectHelper.beforeTaskGraphDone(project, () -> {
            JavaPluginHelper.getJavaCompileTask(project).doFirst(a -> {
                if (extension.addProjectDependencies) {
                    ProjectHelper.getCompileConfiguration(project).getDependencies().forEach(d -> {
                        if (d instanceof ProjectDependency) {
                            String name = (String) ((ProjectDependency) d).getDependencyProject().property("archivesBaseName");
                            if (name == null || name.equals("")) {
                                name = d.getName();
                            }
                            project.getDependencies().add(conf.getName(), d.getGroup() + ":" + name + ":" + d.getVersion());
                        }
                    });
                }
            });

        });
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
