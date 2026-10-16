package org.example.txtimeout;

import java.lang.reflect.Field;
import java.time.Duration;

import jakarta.ejb.Stateless;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.apache.openejb.loader.SystemInstance;

/**
 * Runs inside a container managed transaction (REQUIRED by default) and reports how much time it has left.
 */
@Stateless
public class TransactionTimeoutProbe {

  public Duration remaining() throws Exception {
    final Transaction tx = SystemInstance.get().getComponent(TransactionManager.class).getTransaction();
    // Geronimo stores the absolute deadline of a transaction in TransactionImpl#timeout
    final Field timeout = tx.getClass().getDeclaredField("timeout");
    timeout.setAccessible(true);
    return Duration.ofMillis(timeout.getLong(tx) - System.currentTimeMillis());
  }
}
