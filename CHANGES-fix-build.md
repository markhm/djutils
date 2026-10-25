# Changes on `markhm/djutils` branch `fix/build-junit-jdk25`

Proposed changes for consideration; nothing here has been submitted upstream.
Base: `averbraeck/djutils` `main` at `feab3b88` ("Resolve spotbugs isues").

The changes fall into two independent groups. Group A fixes problems that break `mvn install` on any JDK.
Group B is only needed when building with a JDK newer than 21; it does not change the project's Java 17 target
and can be dropped without affecting group A.

## A. Build fixes (any JDK) — commit `8b432566`

| Area | Problem | Change |
|---|---|---|
| `pom.xml` — JUnit | `djunits` 6.1.0 brings `junit-jupiter-api` 5.13.0 / `junit-platform-commons` 1.13.0 transitively, next to `junit-jupiter-engine` 5.14.4. Surefire fails in `djutils-cli`: *"OutputDirectoryCreator not available; probably due to unaligned versions of the junit-platform-engine and junit-platform-launcher jars"*. | Import `org.junit:junit-bom:${junit.version}` in `<dependencyManagement>`, so all JUnit artifacts resolve to one version. |
| `pom.xml` — compiler | `maven-compiler-plugin` sets only `<source>`/`<target>` 17, so code is compiled against the API of whatever JDK runs the build, not the Java 17 API. | Replace with `<release>${jdk.version}</release>`. The Java 17 target is unchanged; it is now enforced. |
| `djutils-data` — `DataDemo` (test) | Uses `new Time(int, Duration.Unit, Time.Reference)`, which djunits 6.1.0 no longer has; test compilation fails. | Use the 4-argument constructor with `false`, as `TestTextSerializers` already does. |
| `djutils-eval` — `Eval.setUserDefinedFunctions` | `feab3b88` introduced a defensive copy (`new HashMap<>(map)`). This throws an NPE for `null` (used to remove all user functions), and `TestEval.testUserFunctions` fails because it adds a function to the map after installing it. | Copy only when the argument is non-null; `TestEval` installs the map again after adding the function. |
| `pom.xml` — javadoc `<links>` | `djunits.org` and `djutils.org` are currently unreachable, and the `javadoc.io/doc/.../latest/` links for jakarta.annotation and fastcsv redirect, which javadoc treats as an error. | **Temporary workaround:** point the links at versioned `https://javadoc.io/static/<group>/<artifact>/${version}/` URLs. The `djutils-test` link is commented out (not on javadoc.io; no main code references it). Revert once the sites are back if preferred. |

## B. Optional: building with JDK 22+ — commit `b6a320b4`

Only relevant when the build runs on a JDK newer than 21. With JDK 17/21 (as in `maven-publish.yml`) the behaviour
is the same as before.

| Area | Problem | Change |
|---|---|---|
| `djutils-swing/pom.xml` — cacio | `cacio-tta` 1.18 supports JDK 17–21. On newer JDKs the GUI tests fail with `NoClassDefFoundError: sun/java2d/SurfaceManagerFactory`. | Choose the cacio artifact through JDK-activated profiles: `cacio-tta` 1.18 on JDK 17–24, `cacio-tta-jdk25` 2.0 on JDK 25+. On JDK 22–24 (no cacio release) the three `*MultiSliderTest` classes are excluded. |
| `AbstractMultiSlider.setUI` | Creates a UI per inner slider via the UI class's no-arg constructor and ignores failures. JDK 25 removed `BasicSliderUI()`, so `setUI(new BasicSliderUI(...))` silently does nothing there. | Try the `(JSlider)` constructor first, fall back to the no-arg constructor. Same result on JDK 17/21. |
| Swing tests and demos | `new BasicSliderUI()` does not exist in JDK 25. | Use `new BasicSliderUI(null)` (available on all JDKs; the argument is unused). |

## Verification

- JDK 25 (Homebrew OpenJDK 25.0.4.1): `mvn install` including javadoc — `BUILD SUCCESS`, all modules, all tests pass
  (djutils-swing: 28 tests).
- JDK 17/21: not run locally; expected to behave as before apart from group A.
