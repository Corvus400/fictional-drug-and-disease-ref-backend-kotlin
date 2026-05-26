package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeploymentScriptsTest {
    private val projectRoot: Path = Path.of("").toAbsolutePath()

    @Test
    fun `start and stop scripts do not kill unrelated container runtime processes`() {
        val unsafePatterns = listOf(
            "pkill -f \"container-runtime-linux\"",
            "pkill -9 -f \"container-runtime-linux\"",
        )
        val scriptPaths = listOf("scripts/start.sh", "scripts/stop.sh")

        scriptPaths.forEach { scriptPath ->
            val script = projectRoot.resolve(scriptPath).readText()

            unsafePatterns.forEach { pattern ->
                assertFalse(
                    script.contains(pattern),
                    "$scriptPath must not kill every container-runtime-linux process; " +
                        "use named container operations only.",
                )
            }
        }
    }

    @Test
    fun `runtime cleanup is isolated to an explicit force command`() {
        val cleanupScriptPath = projectRoot.resolve("scripts/reset-apple-container-runtime.sh")
        val cleanupScript = cleanupScriptPath.readText()

        assertTrue(Files.isRegularFile(cleanupScriptPath))
        assertTrue(
            cleanupScript.contains("--force"),
            "runtime cleanup must require an explicit force flag.",
        )
        assertTrue(
            cleanupScript.contains("container-runtime-linux"),
            "runtime cleanup script should remain the explicit recovery path for stuck Apple Container runtimes.",
        )
    }

    @Test
    fun `start script verifies the published localhost readiness endpoint before reporting success`() {
        val script = projectRoot.resolve("scripts/start.sh").readText()

        assertTrue(Files.isRegularFile(projectRoot.resolve("scripts/start.sh")))
        assertTrue(
            script.contains("http://127.0.0.1:\${APP_PORT}/health/ready"),
            "scripts/start.sh must verify the same localhost endpoint that users and attack scripts use.",
        )
    }
}
