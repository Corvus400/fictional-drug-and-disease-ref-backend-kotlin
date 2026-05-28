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
            "pkill -f \"pnpm\"",
            "pkill -9 -f \"pnpm\"",
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
        assertTrue(
            script.contains("http://127.0.0.1:\${ADMIN_PORT}/health/ready"),
            "scripts/start.sh must verify the published admin localhost endpoint before reporting success.",
        )
    }

    @Test
    fun `start script publishes admin connector locally and starts CMS with env injection`() {
        val script = projectRoot.resolve("scripts/start.sh").readText()

        assertTrue(
            script.contains("-p \"127.0.0.1:\${ADMIN_PORT}:\${ADMIN_PORT}\""),
            "scripts/start.sh must publish the admin connector only on localhost.",
        )
        assertTrue(
            script.contains("ADMIN_HOST=0.0.0.0"),
            "scripts/start.sh must bind the admin connector inside the container so localhost publishing works.",
        )
        assertTrue(
            script.contains("ALLOW_CONTAINER_ADMIN_WILDCARD_BIND=true"),
            "scripts/start.sh must mark the wildcard admin bind as a container-internal exception.",
        )
        assertTrue(
            script.contains("export VITE_API_BASE_URL=\"http://127.0.0.1:"),
            "scripts/start.sh must inject the CMS API base URL without writing CMS .env.local.",
        )
        assertTrue(
            script.contains("launchctl submit -l \"\$CMS_LAUNCHD_LABEL\""),
            "scripts/start.sh must detach the CMS dev server so it stays running after startup exits.",
        )
        assertTrue(
            script.contains("export PATH=\"\$7\""),
            "scripts/start.sh must pass the caller PATH so pnpm can resolve node under launchd.",
        )
        assertTrue(
            script.contains("tail -f /dev/null | \"\$8\" dev"),
            "scripts/start.sh must keep Vite stdin open when the CMS dev server is launchd-managed.",
        )
        assertTrue(
            script.contains("launchctl submit -l \"\$TUNNEL_LAUNCHD_LABEL\""),
            "scripts/start.sh must detach Cloudflare Tunnel so public mode stays running after startup exits.",
        )
    }

    @Test
    fun `stop script stops only the recorded CMS process`() {
        val script = projectRoot.resolve("scripts/stop.sh").readText()

        assertTrue(
            script.contains("CMS_PID_FILE"),
            "scripts/stop.sh must track the CMS dev server by pid file.",
        )
        assertTrue(
            script.contains("launchctl remove \"\$CMS_LAUNCHD_LABEL\""),
            "scripts/stop.sh must stop the launchd-managed CMS dev server job.",
        )
        assertFalse(
            script.contains("lsof"),
            "scripts/stop.sh must not stop unrelated processes by port scan.",
        )
    }
}
