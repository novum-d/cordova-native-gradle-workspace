import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import com.android.builder.model.v2.models.BasicAndroidProject;

/** Requests Android Studio's Android V2 base models through the Gradle Tooling API. */
public final class ToolingSync {
    public static final class AndroidModels implements BuildAction<List<String>> {
        public List<String> execute(BuildController controller) {
            List<String> models = new ArrayList<>();
            for (BasicGradleProject project : controller.getBuildModel().getProjects()) {
                BasicAndroidProject model = controller.findModel(project, BasicAndroidProject.class);
                if (model != null) {
                    models.add(project.getPath() + " Android V2 model"
                        + " variants=" + model.getVariants().size()
                        + " sources=" + model.getMainSourceSet().getSourceProvider().getManifestFile());
                }
            }
            if (models.isEmpty()) throw new IllegalStateException("No Android models returned");
            return models;
        }
    }
    public static void main(String[] args) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(new File(args[0]))
                .useInstallation(new File(args[1]))
                .connect()) {
            List<String> models = connection.action(new AndroidModels())
                .setJavaHome(new File(System.getenv("JAVA_HOME")))
                .withArguments("--no-watch-fs", "--console=plain")
                .setStandardOutput(System.out).setStandardError(System.err).run();
            models.forEach(System.out::println);
            System.out.println("Android Tooling API model sync succeeded");
        }
    }
}
