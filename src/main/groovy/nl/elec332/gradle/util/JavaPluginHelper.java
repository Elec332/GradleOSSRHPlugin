package nl.elec332.gradle.util;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.internal.jvm.Jvm;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Created by Elec332 on 31-3-2020
 */
public class JavaPluginHelper {

    @Nonnull
    public static String getJavaHome() {
        return Jvm.current().getJavaHome().getAbsolutePath();
    }

    @Nonnull
    public static Task getClassesTask(Project project) {
        return Objects.requireNonNull(ProjectHelper.getTaskByName(project, JavaPlugin.CLASSES_TASK_NAME));
    }

    @Nonnull
    public static JavaCompile getJavaCompileTask(Project project) {
        return Objects.requireNonNull((JavaCompile) ProjectHelper.getTaskByName(project, JavaPlugin.COMPILE_JAVA_TASK_NAME));
    }

    @Nonnull
    public static Javadoc getJavaDocTask(Project project) {
        return Objects.requireNonNull((Javadoc) ProjectHelper.getTaskByName(project, JavaPlugin.JAVADOC_TASK_NAME));
    }

    @Nonnull
    public static JavaPluginConvention getJavaConvention(Project project) {
        return Objects.requireNonNull(project.getConvention().getPlugin(JavaPluginConvention.class));
    }

    @Nonnull
    public static SourceSet getMainJavaSourceSet(Project project) {
        return Objects.requireNonNull(getJavaConvention(project).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME));
    }

}
