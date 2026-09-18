package com.schwab.assignment.orchestration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineeringArtifactsTest {
    @TempDir
    Path workspace;

    @Test
    void brownfieldArtifactListsScannedComponentsAndObservedDataFlow() throws Exception {
        write("src/main/java/example/UrlController.java", "@RestController\nclass UrlController { UrlService service; }");
        write("src/main/java/example/UrlService.java", "class UrlService { ShortUrlRepository repository; }");
        write("src/main/java/example/ShortUrl.java", "@Entity\nclass ShortUrl {}");
        write("src/main/java/example/ShortUrlRepository.java", "interface ShortUrlRepository { ShortUrl find(); }");
        write("src/test/java/example/UrlControllerTest.java", "class UrlControllerTest {}");
        EngineeringArtifacts artifacts = new EngineeringArtifacts(workspace);
        Workflow workflow = new Workflow("brownfield", "Add redirect analytics", Map.of());

        artifacts.create(workflow);

        String report = Files.readString(workspace.resolve("work/generated/" + workflow.id() + "-impact-analysis.md"));
        assertAll(
                () -> assertTrue(report.contains("UrlController.java")),
                () -> assertTrue(report.contains("UrlService.java")),
                () -> assertTrue(report.contains("ShortUrl.java")),
                () -> assertTrue(report.contains("ShortUrlRepository.java")),
                () -> assertTrue(report.contains("UrlControllerTest.java")),
                () -> assertTrue(report.contains("UrlController -> UrlService -> ShortUrlRepository -> ShortUrl (H2)")));
    }

    @Test
    void detectsExpiryAndGeneralQualitativeAmbiguityButAllowsMeasuredRequirements() {
        EngineeringArtifacts artifacts = new EngineeringArtifacts(workspace);
        assertTrue(artifacts.isAmbiguous("Make links expire"));
        assertTrue(artifacts.isAmbiguous("Make redirects faster"));
        assertTrue(artifacts.isAmbiguous("Improve reliability"));
        assertFalse(artifacts.isAmbiguous("Links expire after 30 days"));
        assertFalse(artifacts.isAmbiguous("Make redirects faster than 200 ms"));
    }

    private void write(String relative, String content) throws Exception {
        Path file = workspace.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
