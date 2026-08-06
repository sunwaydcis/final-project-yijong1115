# Citations and Third-Party Resources

This project uses the following external programming languages, libraries and development tools. No third-party source-code snippets were copied directly into the project.

## 1. Scala 3

- **Resource:** Scala 3.8.4
- **Author/Organisation:** Scala Center and Scala contributors
- **Source:** https://www.scala-lang.org/download/3.8.4.html
- **Licence:** Apache License 2.0
- **Purpose:** Main programming language used throughout the application
- **Affected files:** `build.sbt` and all files under `src/main/scala/`

## 2. ScalaFX

- **Resource:** ScalaFX 21.0.0-R32
- **Author/Organisation:** ScalaFX contributors
- **Source:** https://github.com/scalafx/scalafx
- **Licence:** BSD 3-Clause License
- **Purpose:** Scala-based user-interface framework used to build the desktop application
- **Affected files:** `build.sbt`, `src/main/scala/foodpantry/MainApp.scala` and files under `src/main/scala/foodpantry/ui/`

## 3. Apache Derby

- **Resource:** Apache Derby 10.17.1.0
- **Author/Organisation:** Apache Software Foundation
- **Source:** https://db.apache.org/derby/releases/release-10_17_1_0.cgi
- **Licence information:** https://db.apache.org/derby/license.html
- **Licence:** Apache License 2.0
- **Purpose:** Embedded relational database used to persist food items, household requests and distribution plans
- **Affected files:** `build.sbt`, `src/main/scala/foodpantry/database/`, repository classes and transaction-related services

## 4. MUnit

- **Resource:** MUnit 1.0.2
- **Author/Organisation:** Scalameta contributors
- **Source:** https://github.com/scalameta/munit
- **Documentation:** https://scalameta.org/munit/
- **Licence:** Apache License 2.0
- **Purpose:** Automated unit and workflow testing
- **Affected files:** `build.sbt` and files under `src/test/scala/`

## 5. diagrams.net / draw.io

- **Resource:** diagrams.net UML diagramming tool
- **Author/Organisation:** draw.io Ltd and draw.io AG
- **Source:** https://app.diagrams.net/
- **Project source:** https://github.com/jgraph/drawio
- **Licence:** Apache License 2.0
- **Purpose:** Used to create the UML class diagram
- **Affected file:** `docs/UML.png`
- **Note:** The UML content and arrangement were created specifically for this project. No external diagram template or third-party icon was included in the application.

## Original Project Resources

The following resources were created specifically for this project:

- `src/main/resources/styles.css`
- Application source code
- Test code
- UML diagram content
- Documentation and reflections

AI assistance used when developing these resources is disclosed separately in `ai/declaration.md`, `ai/interaction_log.md` and `docs/ai_reflection.md`.

No external dataset, image, icon, font or prebuilt CSS theme was used.
