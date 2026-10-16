package org.example.txtimeout;

import java.time.Duration;
import java.util.Properties;

import jakarta.ejb.embeddable.EJBContainer;
import org.apache.openejb.OpenEjbContainer;

/**
 * Boots the embedded container from the shaded jar and prints the time left in a container managed transaction.
 */
public final class Launcher {

  private static final String APP_NAME = "tx-timeout-example";

  /** Pinned in META-INF/ejb-jar.xml, otherwise it is derived from the jar or directory name. */
  private static final String MODULE_NAME = "tx-timeout";

  private Launcher() {
  }

  public static void main(final String[] args) throws Exception {
    final Properties properties = new Properties();
    properties.setProperty(OpenEjbContainer.APP_NAME, APP_NAME);
    properties.setProperty(OpenEjbContainer.PROVIDER, OpenEjbContainer.class.getName());
    properties.setProperty(OpenEjbContainer.OPENEJB_EMBEDDED_REMOTABLE, "false");
    properties.setProperty("ejbd.disabled", "true");
    properties.setProperty("ejbds.disabled", "true");
    properties.setProperty("admin.disabled", "true");
    properties.setProperty("openejb.jaxrs.application", "false");

    try (EJBContainer container = EJBContainer.createEJBContainer(properties)) {
      final TransactionTimeoutProbe probe = (TransactionTimeoutProbe) container.getContext()
          .lookup("java:global/" + APP_NAME + "/" + MODULE_NAME + "/TransactionTimeoutProbe");
      final Duration remaining = probe.remaining();
      System.out.printf("Remaining transaction time: %d minutes (%d ms)%n",
          Math.round(remaining.toMillis() / 60_000d), remaining.toMillis());
    }
  }
}
