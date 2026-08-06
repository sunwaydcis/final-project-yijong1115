# Food Pantry Inventory and Demand Management System

## Student Information

**Student Name:** Gan Yi Jong  
**Student ID:** 23051725  
**Module:** PRG2104 Object-Oriented Programming  
**Theme:** SDG 1 - No Poverty  
**Sub-domain:** Food Pantry Inventory and Demand  

## Project Summary

The Food Pantry Inventory and Demand Management System is a standalone ScalaFX desktop application designed to support food pantry coordinators.

The application records donated food, manages household requests, monitors stock and expiry conditions, and prepares daily distribution plans. Its purpose is to improve the organisation of food assistance while reducing shortages and unnecessary food waste.

## Implemented Features

### 1. Donated Food Inventory Management

Users can:

- Add perishable and shelf-stable food items
- See each item's food type and automatic storage instruction
- Use a required use-by date for perishable food or an optional best-before date for shelf-stable food
- Select stock units such as packs, bottles, cans, kilograms or litres from a validated list
- Validate food categories as descriptive text rather than numeric values
- Normalise category casing so values such as `meat` and `Meat` appear as one category
- View all stored food items
- Update an item's available quantity
- Delete selected food items
- Store expiry dates, units and categories
- Retain inventory data after restarting the application

### 2. Household and Food Request Management

Users can:

- Register household food requests
- Choose requested categories directly from current inventory
- Record household names and requested quantities
- View all requests
- Approve or reject pending requests with clear actions
- Keep rejected requests out of distribution planning
- Automatically mark requests Fulfilled after the full quantity is distributed
- Retain request data after restarting the application

### 3. Expiry and Low-Stock Monitoring

The dashboard calculates and displays:

- Total food records
- Low-stock item count
- Expiry-alert item count
- Unique items requiring attention

The monitoring logic uses immutable collection operations and polymorphic food-item behaviour.

### 4. Daily Distribution Planning and Reporting

Users can:

- Create distribution plans from approved household requests
- Select an available food item
- Validate requested quantities and distribution dates
- Complete or cancel Planned distributions through guided actions
- Reduce inventory automatically when a distribution is completed
- Release reserved stock when a distribution is cancelled
- View totals for each distribution status
- View the number of requests receiving completed distributions
- Retain distribution plans and status changes after restarting

## Object-Oriented Design

The project demonstrates:

- Traits and sealed traits
- Inheritance
- Subtype polymorphism
- Parametric polymorphism through `Repository[T <: Entity]`
- Method overriding
- Encapsulation using private members
- Immutable case classes
- Immutable updates using `copy`
- Enumeration types for request and distribution statuses
- Helper objects for validation, mapping, database setup and reporting

Examples include the `FoodItem` hierarchy, generic repository interface, inventory-alert hierarchy and Derby repository implementations.

## Functional Programming Techniques

The application uses Scala collection and error-handling features including:

- `map`
- `filter`
- `flatMap`
- `groupMapReduce`
- `distinct`
- `foldLeft`
- `Option`
- `Either`
- `Try`
- Pattern matching

Mutable `var` declarations and mutable collection classes are not used in the production source code.

## Technology

- Scala 3.8.4
- ScalaFX 21.0.0-R32
- Java 21 target environment
- sbt 2.0.3
- Apache Derby 10.17.1.0 embedded database
- MUnit 1.0.2
- Git and GitHub

## Project Structure

```text
Project_23051725/
|-- ai/
|   |-- declaration.md
|   `-- interaction_log.md
|-- docs/
|   |-- UML.png
|   |-- ai_reflection.md
|   |-- citations.md
|   |-- dev_log.md
|   `-- reflection.md
|-- project/
|   `-- build.properties
|-- src/
|   |-- main/
|   |   |-- resources/
|   |   |   `-- styles.css
|   |   `-- scala/
|   |       `-- foodpantry/
|   |           |-- database/
|   |           |-- model/
|   |           |-- repository/
|   |           |-- service/
|   |           |-- ui/
|   |           `-- MainApp.scala
|   `-- test/
|       `-- scala/
|           `-- foodpantry/
|               |-- DistributionReportServiceSuite.scala
|               |-- DistributionWorkflowSuite.scala
|               |-- FoodCategoriesSuite.scala
|               |-- FoodItemValidatorSuite.scala
|               |-- HouseholdRequestWorkflowSuite.scala
|               `-- InventoryDashboardSummaryServiceSuite.scala
|-- build.sbt
|-- README.md
`-- submission_manifest.md
```

## Database Storage

The application uses an embedded Apache Derby database.

The database is created automatically at:

```text
data/foodPantryDB
```

The following tables are created automatically when the application starts:

- `FOOD_ITEMS`
- `HOUSEHOLD_REQUESTS`
- `DISTRIBUTION_PLANS`

The generated database folder and Derby log are excluded from Git because they contain local runtime data.

## Setup Requirements

Before running the project, install:

1. Java Development Kit 21
2. sbt
3. Git
4. IntelliJ IDEA with Scala support, or another Scala-compatible editor

Confirm Java is available:

```powershell
java -version
```

Confirm sbt is available:

```powershell
sbt --version
```

## Compile the Application

From the project root, run:

```powershell
sbt clean compile
```

## Run the Application

From the project root, run:

```powershell
sbt run
```

The database schema is initialised automatically when the application starts.

## Suggested Application Workflow

1. Open **Food Inventory** and add donated food items.
2. Open **Household Requests** and create a household request.
3. Select the request and click **Approve Selected Request**.
4. Open **Distribution Plan**; its available choices refresh automatically.
5. Create a distribution plan for the approved request.
6. Select the plan and click **Complete Selected Plan** or **Cancel Selected Plan**.
7. Review distribution totals and requests served.
8. Open **Dashboard** to review inventory and alert information.

Each screen refreshes its records automatically when opened from the
sidebar. The visible Refresh buttons remain available for manual reloads.

## Verification Completed

The project has been checked using:

```powershell
sbt clean compile
sbt test
```

The source-code audit confirmed:

- No mutable `var` declarations
- No mutable collection imports
- No direct JavaFX imports
- No unsafe `Option.get` calls
- 16 automated tests covering validation, normalisation, request decisions,
  dashboard summaries, distribution reporting and stock workflows
- Persistence of inventory, household requests and distribution plans after restart

The final submission should also be tested using Java 21.

## AI Usage Summary

AI assistance was used under the module's Tier C AI-Integrated policy.

The AI interaction log records prompts, suggestions, student decisions and affected source files. AI-generated suggestions were reviewed, modified, compiled and tested before being accepted.

Current recorded evidence includes:

- 57 entries in `ai/interaction_log.md`
- AI-assisted source references linked to matching log entries

Full details are available in:

```text
ai/interaction_log.md
ai/declaration.md
docs/ai_reflection.md
```

## Submission Items Still to Complete

Before final submission:

- Complete the personal reflection
- Complete the AI reflection
- Create and add the UML diagram
- Record the demonstration video
- Test the final project using Java 21
- Update `submission_manifest.md`
