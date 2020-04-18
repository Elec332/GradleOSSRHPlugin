package nl.elec332.gradle.ossrhplugin

import groovy.transform.VisibilityOptions
import groovy.transform.options.Visibility
import nl.elec332.gradle.util.GroovyHooks
import nl.elec332.gradle.util.Utils
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployer

/**
 * Created by Elec332 on 18-4-2020
 */
class MavenConfigurator {
    
    static void configure(Project project, Closure closure, String name) {
        GroovyHooks.configureMaven(project, { maven ->
            maven.pom.project {
                "$name"(closure)
            }
        })
    }

    static void configureLicense(Project project, Closure closure) {
        configure(project, {
            license(closure)
        }, "licenses")
    }

    static void configureDeveloper(Project project, Closure closure) {
        configure(project, {
            developer(closure)
        }, "developers")
    }

    static void configureContributor(Project project, Closure closure) {
        configure(project, {
            contributor(closure)
        }, "contributors")
    }

    static void configureMaven(MavenDeployer deployer, OSSRHExtension extension, Project project) {
        validateExtension(extension, project)
        boolean userPass = false
        String user, pass
        if (project.hasProperty(OSSRHPlugin.OSSRH_USERNAME_PROPERTY) && project.hasProperty(OSSRHPlugin.OSSRH_USERNAME_PROPERTY)) {
            userPass = true;
            user = project.property(OSSRHPlugin.OSSRH_USERNAME_PROPERTY)
            pass = project.property(OSSRHPlugin.OSSRH_PASSWORD_PROPERTY)
        } else if (project.hasProperty("ossrhUsername") && project.hasProperty("ossrhPassword")) {
            userPass = true
            user = project.property("ossrhUsername")
            pass = project.property("ossrhPassword")
        }

        if (!userPass) {
            println "No credentials found..."
        }

        String repo = extension.repositoryUrl;
        if (!Utils.isNullOrEmpty(extension.snapshotRepositoryUrl)) {
            if (extension.useSnapshotRepo) {
                deployer.repository(url: extension.snapshotRepositoryUrl) {
                    if (userPass) {
                        authentication(userName: user, password: pass)
                    }
                }
            } else if (extension.snapshotFilter != null && extension.snapshotFilter.test((String) project.version)) {
                repo = extension.snapshotRepositoryUrl
            }
        }

        deployer.repository(url: repo) {
            if (userPass) {
                authentication(userName: user, password: pass)
            }
        }

        deployer.pom.project {
            name extension.name
            artifactId extension.artifactId
            packaging extension.packaging
            description extension.description
            url extension.url

            if (!Utils.isNullOrEmpty(extension.inceptionYear)) {
                inceptionYear extension.inceptionYear
            }

            scm {
                url extension.scmUrl
                connection extension.scmConnection
                developerConnection extension.scmDevConnection
            }

            if (!Utils.isNullOrEmpty(extension.githubUrl) && extension.useGithubIssues) {
                issueManagement {
                    system "Github"
                    url extension.githubUrl + "/issues"
                }
            }

        }

        List test = deployer.pom.getModel().getLicenses()
        if (test.isEmpty()) {
            throw new MissingPropertyException("No license(s) defined!")
        }
        test = deployer.pom.getModel().getDevelopers()
        if (test.isEmpty()) {
            throw new MissingPropertyException("No developer(s) defined!")
        }
    }

    @VisibilityOptions(Visibility.PRIVATE)
    static void validateExtension(OSSRHExtension extension, Project project) {
        if (Utils.isNullOrEmpty(extension.repositoryUrl)) {
            throw new MissingPropertyException("Maven repo URL not defined!")
        }
        if (Utils.isNullOrEmpty(extension.name)) {
            extension.name = project.archivesBaseName
        }
        if (Utils.isNullOrEmpty(extension.artifactId)) {
            extension.artifactId = extension.name.toLowerCase(Locale.ROOT)
        }
        if (Utils.isNullOrEmpty(extension.packaging)) {
            throw new MissingPropertyException("Packaging type not defined!")
        }
        if (Utils.isNullOrEmpty(extension.description)) {
            throw new MissingPropertyException("No project description provided!")
        }

        boolean hasGitHub = !Utils.isNullOrEmpty(extension.githubUrl)
        if (hasGitHub) {
            if (!extension.githubUrl.contains(OSSRHPlugin.GITHUB_BASE_URL)) {
                throw new IllegalArgumentException("Github URL does not contain " + OSSRHPlugin.GITHUB_BASE_URL)
            }
            String projectLink = extension.githubUrl.split(OSSRHPlugin.GITHUB_BASE_URL)[1]
            if (Utils.isNullOrEmpty(extension.url)) {
                extension.url = extension.githubUrl
            }
            if (Utils.isNullOrEmpty(extension.scmUrl)) {
                extension.scmUrl = extension.githubUrl
            }
            if (Utils.isNullOrEmpty(extension.scmConnection)) {
                extension.scmConnection = "scm:git:git://" + OSSRHPlugin.GITHUB_BASE_URL + projectLink + ".git"
            }
        }
        if (Utils.isNullOrEmpty(extension.url)) {
            throw new MissingPropertyException("No project URL defined!")
        }
        if (Utils.isNullOrEmpty(extension.scmUrl)) {
            throw new MissingPropertyException("No SCM property defined!")
        }
        if (Utils.isNullOrEmpty(extension.scmConnection)) {
            throw new MissingPropertyException("No SCM connection method defined!")
        }
        if (Utils.isNullOrEmpty(extension.scmDevConnection)) {
            if (extension.scmConnection.contains("git://")) {
                extension.scmDevConnection = extension.scmConnection.replace("git://", "ssh://git@")
            } else {
                throw new MissingPropertyException("No SCM developer connection method defined!")
            }
        }
    }

}
