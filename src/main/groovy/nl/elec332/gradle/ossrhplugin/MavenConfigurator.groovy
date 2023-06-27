package nl.elec332.gradle.ossrhplugin

import nl.elec332.gradle.util.MavenHooks
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication

import java.util.function.Supplier

/**
 * Created by Elec332 on 18-4-2020
 */
class MavenConfigurator {

    static void configure(Project project, Action<MavenPom> pom) {
        try {
            MavenHooks.configureMaven(project, { maven ->
                maven.pom(pom)
            })
        } catch(Exception e) {
            e.printStackTrace()
        }
        project.dependencies {

        }
    }

    static void configureMaven(MavenPublication deployer, OSSRHExtension extension, Project project, Supplier<String> localMaven) {
        MavenConfiguratorJava.configureMaven(deployer, extension, project, localMaven);
        List test = deployer.pom.getLicenses()
        if (test.isEmpty()) {
            throw new MissingPropertyException("No license(s) defined!")
        }
        test = deployer.pom.getDevelopers()
        if (test.isEmpty()) {
            throw new MissingPropertyException("No developer(s) defined!")
        }
    }

}
