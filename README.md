# OpenEJB: override the transaction manager defaults in a shaded jar

An application that boots OpenEJB with `EJBContainer.createEJBContainer(...)` and is shipped as a
single shaded (uber) jar has no `conf/openejb.xml` on disk. This example shows how to change the
defaults of a built-in service anyway, here the default transaction timeout, from 10 to 30 minutes.

## The idea

OpenEJB's built-in services are declared in `service-jar.xml` files inside `openejb-core`:

- `META-INF/org.apache.openejb/service-jar.xml` defines the services and their defaults
  (`defaultTransactionTimeout 10 minutes` for the `Default Transaction Manager`).
- `META-INF/org.apache.openejb.embedded/service-jar.xml` is used when OpenEJB runs embedded
  (`EJBContainer`, `LocalInitialContextFactory`). Each of its entries only points to a parent in the
  file above, and a property set on the child overrides the one of the parent.

So the project ships its own copy of the embedded file with one entry changed
([`service-jar.xml`](src/main/resources/META-INF/org.apache.openejb.embedded/service-jar.xml)):

```xml
<ServiceProvider id="Default Transaction Manager" parent="org.apache.openejb:Default Transaction Manager">
  defaultTransactionTimeout = 30 minutes
</ServiceProvider>
```

and lets the shade plugin drop the original from `openejb-core` ([`pom.xml`](pom.xml)):

```xml
<filter>
  <artifact>org.apache.tomee:openejb-core</artifact>
  <excludes>
    <exclude>META-INF/org.apache.openejb.embedded/service-jar.xml</exclude>
  </excludes>
</filter>
```

The same works for any other property of any built-in service (containers, resources, ...).

## Run it

```bash
mvn verify
java -jar target/tx-timeout-example.jar
```

```
Remaining transaction time: 30 minutes (1799998 ms)
```

`mvn verify` runs [`ShadedJarIT`](src/test/java/org/example/txtimeout/ShadedJarIT.java), which starts
the shaded jar with `java -jar` and asserts the 30 minutes. The value is read inside a container
managed transaction by [`TransactionTimeoutProbe`](src/main/java/org/example/txtimeout/TransactionTimeoutProbe.java).

## Things to know

- **It only works in the shaded jar.** OpenEJB reads *every* `service-jar.xml` it finds on the
  classpath and, for the same id, the one read last wins. On a plain classpath (IDE, unit tests) that is
  the copy in `openejb-core`, so you still get 10 minutes there. The shade filter is what makes the
  override the only definition.
- **Keep the copy in sync.** When you upgrade OpenEJB/TomEE, compare your copy with the new upstream file.
- **Shading OpenEJB needs `OpenWebBeansPropertiesTransformer`**, otherwise the
  `META-INF/openwebbeans/openwebbeans.properties` files of OWB and OpenEJB overwrite each other and CDI
  fails to start (`NullPointerException: Name is null` in `BeanDiscoveryMode.valueOf`).
- **`META-INF/scan.xml`** limits scanning to the application package; the uber jar is one single module
  and would otherwise get the container internals scanned too.
- **Don't name the jar `openejb-*`.** OpenEJB never deploys a jar (or `target/classes` of a project)
  whose name starts with `openejb-` (`DeploymentsResolver`), and also skips the prefixes listed in its
  `default.exclusions`. That is why the shaded jar is written to `target/tx-timeout-example.jar`.

## Alternatives

If a file next to the jar is acceptable, a regular configuration works without touching provider files:

- `conf/openejb.xml` (pass its location with `-Dopenejb.configuration=...`):

  ```xml
  <openejb>
    <TransactionManager id="Default Transaction Manager" type="TransactionManager">
      defaultTransactionTimeout = 30 minutes
    </TransactionManager>
  </openejb>
  ```

- a system property, or an entry in the properties passed to `EJBContainer.createEJBContainer(...)`:
  `Default Transaction Manager.defaultTransactionTimeout=30 minutes`

`openejb-jar.xml` does **not** work: the transaction manager is a container wide service created when the
container boots, before any module descriptor is read.
