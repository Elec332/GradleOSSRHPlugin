package nl.elec332.gradle.ossrhplugin;

import groovy.lang.MissingPropertyException;
import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.MavenHooks;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.publish.maven.MavenPublication;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Created by Elec332 on 1/29/2021
 */
public class MavenConfiguratorJava {

    static void configureMaven(MavenPublication deployer, OSSRHExtension extension, Project project, Supplier<String> localMaven) {
        MavenConfiguratorJava.validateExtension(extension, project, localMaven);
        deployer.from(project.getComponents().getByName("java"));

        boolean userPass = false;
        String user = null, pass = null;
        if (project.hasProperty(OSSRHPlugin.OSSRH_USERNAME_PROPERTY) && project.hasProperty(OSSRHPlugin.OSSRH_USERNAME_PROPERTY)) {
            userPass = true;
            user = (String) project.property(OSSRHPlugin.OSSRH_USERNAME_PROPERTY);
            pass = (String) project.property(OSSRHPlugin.OSSRH_PASSWORD_PROPERTY);
        } else if (project.hasProperty("ossrhUsername") && project.hasProperty("ossrhPassword")) {
            userPass = true;
            user = (String) project.property("ossrhUsername");
            pass = (String) project.property("ossrhPassword");
        }

        if (!userPass) {
            System.out.println("No credentials found...");
        }

        String repo = extension.repositoryUrl;
        if (extension.testLocalRepo || extension.testFilter != null && extension.testFilter.test(GroovyHooks.getVersion(project))) {
            repo = extension.localRepository;
        } else if (!Utils.isNullOrEmpty(extension.snapshotRepositoryUrl)) {
            if (extension.useSnapshotRepo) {
                repo = extension.snapshotRepositoryUrl;
            } else if (extension.snapshotFilter != null && extension.snapshotFilter.test(GroovyHooks.getVersion(project))) {
                repo = extension.snapshotRepositoryUrl;
            }
        }
        setMavenRepo(project, repo, userPass, user, pass);


        deployer.setArtifactId(extension.artifactId);

        deployer.pom(pom -> {
            pom.getName().set(extension.name.toLowerCase(Locale.ROOT));
            pom.setPackaging(extension.packaging);
            pom.getDescription().set(extension.description);
            pom.getUrl().set(extension.url);
            if (!Utils.isNullOrEmpty(extension.inceptionYear)) {
                pom.getInceptionYear().set(extension.inceptionYear);
            }
            pom.scm(scm -> {
                scm.getUrl().set(extension.scmUrl);
                scm.getConnection().set(extension.scmConnection);
                scm.getDeveloperConnection().set(extension.scmDevConnection);
            });
            if (!Utils.isNullOrEmpty(extension.githubUrl) && extension.useGithubIssues) {
                pom.issueManagement(issueManagement -> {
                    issueManagement.getSystem().set("Github");
                    issueManagement.getUrl().set(extension.githubUrl + "/issues");
                });
            }
        });
    }

    private static void setMavenRepo(Project project, String repo, boolean userPass, String user, String pass) {
        MavenHooks.setMavenRepository(project, mavenRepo -> {
            mavenRepo.setUrl(repo);
            if (userPass && !mavenRepo.getUrl().getScheme().equals("file")) {
                mavenRepo.credentials(auth -> {
                        auth.setPassword(pass);
                        auth.setUsername(user);
                });
            }
        });
    }

    private static void validateExtension(OSSRHExtension extension, Project project, Supplier<String> localMaven) {
        if (Utils.isNullOrEmpty(extension.repositoryUrl)) {
            throw new MissingPropertyException("Maven repo URL not defined!");
        }
        if (Utils.isNullOrEmpty(extension.name)) {
            extension.name = ((BasePluginConvention) project.getConvention().getPlugins().get("base")).getArchivesBaseName();
        }
        if (Utils.isNullOrEmpty(extension.artifactId)) {
            extension.artifactId = extension.name.toLowerCase(Locale.ROOT);
        }
        if (Utils.isNullOrEmpty(extension.packaging)) {
            throw new MissingPropertyException("Packaging type not defined!");
        }
        if (Utils.isNullOrEmpty(extension.description)) {
            throw new MissingPropertyException("No project description provided!");
        }
        if (Utils.isNullOrEmpty(extension.localRepository)) {
            extension.localRepository = localMaven.get();
        }

        boolean hasGitHub = !Utils.isNullOrEmpty(extension.githubUrl);
        if (hasGitHub) {
            if (!extension.githubUrl.contains(OSSRHPlugin.GITHUB_BASE_URL)) {
                throw new IllegalArgumentException("Github URL does not contain " + OSSRHPlugin.GITHUB_BASE_URL);
            }
            String projectLink = extension.githubUrl.split(OSSRHPlugin.GITHUB_BASE_URL)[1];
            if (Utils.isNullOrEmpty(extension.url)) {
                extension.url = extension.githubUrl;
            }
            if (Utils.isNullOrEmpty(extension.scmUrl)) {
                extension.scmUrl = extension.githubUrl;
            }
            if (Utils.isNullOrEmpty(extension.scmConnection)) {
                extension.scmConnection = "scm:git:git://" + OSSRHPlugin.GITHUB_BASE_URL + projectLink + ".git";
            }
        }
        if (Utils.isNullOrEmpty(extension.url)) {
            throw new MissingPropertyException("No project URL defined!");
        }
        if (Utils.isNullOrEmpty(extension.scmUrl)) {
            throw new MissingPropertyException("No SCM property defined!");
        }
        if (Utils.isNullOrEmpty(extension.scmConnection)) {
            throw new MissingPropertyException("No SCM connection method defined!");
        }
        if (Utils.isNullOrEmpty(extension.scmDevConnection)) {
            if (extension.scmConnection.contains("git://")) {
                extension.scmDevConnection = extension.scmConnection.replace("git://", "ssh://git@");
            } else {
                throw new MissingPropertyException("No SCM developer connection method defined!");
            }
        }
    }
}
