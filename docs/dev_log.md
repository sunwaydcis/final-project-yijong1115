# Development Log

## 2026-07-21

- Accepted the PRG2104 Final Project assignment through GitHub Classroom.
- Connected the local `Project_23051725` folder to the GitHub repository.
- Configured the initial Scala and sbt project.
- Created the required submission folder structure.
- Selected the Community Food Aid Planner project idea.
- Identified four planned application features.
- Created the initial project README.

## 2026-07-24

- Reviewed the lecturer’s recommended SDG-1 project sub-domains.
- Changed the initial project scope from Community Food Aid Planner to Food Pantry Inventory & Demand.
- Confirmed the final project title as Food Pantry Inventory and Demand Management System.
- Refined the four planned features to focus on inventory, household requests, expiry monitoring and distribution planning.
- Added ScalaFX 21 and embedded Apache Derby dependencies.
- Created the initial `MainApp` object using `JFXApp3`.
- Added a `PrimaryStage`, `Scene`, `BorderPane` and centre label.
- Confirmed that the application compiled and opened successfully.
- Added four placeholder screens: Dashboard, Food Inventory, Household Requests and Distribution Plan.
- Added sidebar navigation using ScalaFX buttons and `onAction = handle { ... }`.
- Tested all four navigation buttons successfully.
- Added targeted compiler-warning settings for the ScalaFX `handle` helper.
- Added the reusable `Entity` trait with an abstract `id`.
- Prepared the model layer for future generic repositories and domain classes.
- Confirmed the project compiled successfully after adding the trait.
- Added the immutable `FoodItem` hierarchy.
- Added `PerishableFood` and `ShelfStableFood` case-class subtypes.
- Used `Option[LocalDate]` to represent optional expiry information safely.
- Added overridden storage instructions to demonstrate subtype polymorphism.
- Confirmed the project compiled successfully.
- Added the generic `Repository[T <: Entity]` contract.
- Added add, update, delete, find-by-ID and find-all operations.
- Used `Try`, `Option` and immutable `List` return types for safe error handling.
- Confirmed the project compiled successfully.
- Added the embedded Derby `DatabaseManager`.
- Encapsulated the database URL using a private value.
- Added a generic `withConnection[T]` method using `Try`.
- Ensured JDBC connections are closed using `try` and `finally`.
- Confirmed the project compiled successfully.

## 2026-07-25
- Tested the embedded Derby connection and received `Success(true)`.
- Confirmed that Derby created the persistent database under `data/foodPantryDB`.
- Updated `.gitignore` so generated Derby database files and `derby.log` are not committed.
- Added an idempotent `DatabaseInitializer`.
- Added the `FOOD_ITEMS` Derby table schema.
- Used database metadata to avoid recreating an existing table.
- Ensured JDBC statements and result sets are closed safely.
- Confirmed the project compiled successfully.
- Added the `FOOD_ITEMS` Derby schema initializer.
- Encountered a Derby class-loader error while testing through `sbt console`.
- Reviewed the stack trace and moved initialization testing to the forked ScalaFX application.
- Confirmed that the database initialized successfully and the application opened normally.
- Added `FoodItemMapper` to convert Derby result rows into `FoodItem` subtypes.
- Used pattern matching for `PERISHABLE` and `SHELF_STABLE` item types.
- Used `Option` for nullable expiry dates and `Either` for unsupported or invalid records.
- Removed a hidden UTF-8 BOM character created by PowerShell.
- Confirmed the project compiled successfully.
- Added `FoodItemValidator` for immutable domain validation.
- Checked required ID, name, category and unit fields.
- Added quantity validation to require a value greater than zero.
- Used `Either[List[String], FoodItem]` to return all validation errors safely.
- Confirmed the project compiled successfully.
- Added `DerbyFoodItemRepository` implementing `Repository[FoodItem]`.
- Added database operations to create, update, delete and retrieve food items.
- Used prepared statements and safely closed JDBC resources.
- Integrated `FoodItemValidator` and `FoodItemMapper`.
- Used subtype pattern matching for perishable and shelf-stable food.
- Confirmed the project compiled successfully.
- Ran a forked repository smoke test and confirmed add, find, update and delete operations all succeeded.
- Added a read-only ScalaFX `InventoryView`.
- Displayed persisted food items using `TableView` and `ObservableBuffer`.
- Added a refresh button and repository-loading status message.
- Used the generic `Repository[FoodItem]` contract instead of depending directly on Derby.
- Confirmed the project compiled successfully.
- Connected `InventoryView` to `MainApp` and confirmed that the empty Derby inventory loaded as `0 food item(s)` without errors.
- Added `FoodItemForm` for creating perishable and shelf-stable food records.
- Connected the form to the inventory table and Derby repository.
- Successfully added `Fresh Milk` and automatically refreshed the table.
- Restarted the application and confirmed that the saved record remained in the embedded database.
- Added deletion for the selected inventory record.
- Confirmed the no-selection warning appears correctly.
- Added and deleted a temporary shelf-stable item successfully.
- Confirmed that other persisted records remained unchanged.
- Added quantity updating for the selected inventory item.
- Used safe whole-number parsing and positive-quantity validation.
- Used subtype-specific `copy` operations to preserve immutable domain objects.
- Updated `Fresh Milk` from quantity 12 to 20.
- Restarted the application and confirmed that the updated quantity remained persisted.

