package org.example.txtimeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Starts the shaded jar as its own JVM, exactly like it is run in production.
 */
class ShadedJarIT {

  @Test
  void shadedJarUsesThirtyMinutes() throws Exception {
    final File jar = new File(System.getProperty("shaded.jar"));
    assertTrue(jar.isFile(), "missing " + jar);

    final String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    final Process process = new ProcessBuilder(java, "-jar", jar.getAbsolutePath())
        .redirectErrorStream(true)
        .start();
    final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(process.waitFor(2, TimeUnit.MINUTES), "shaded jar did not stop");

    assertEquals(0, process.exitValue(), output);
    assertTrue(output.contains("Remaining transaction time: 30 minutes"), output);
  }
}
