# Conference Management Desktop Application (Skeleton)

This repository contains a **minimal scaffold** for a Java 17 / JavaFX 20 application backed by MySQL.

## Quick Start

```bash
# Ensure Java 17 and Maven 3.9+ are installed
mvn clean javafx:run
```

The default `DBUtil` expects a local database called `conference_db`
with user `conference_user` / `your_password`.  
Adjust credentials in `DBUtil.java` or via environment variables.

## Project layout

* `src/main/java` – Java sources  
* `src/main/resources` – FXML, CSS, images  
* `sql/schema.sql` – initial schema  
* `data/` – CSV datasets for bulk import

## Next steps

1. Design each FXML scene following the style guide (logo, palette, Comic Sans MS).
2. Implement models and DAOs under `com.example.conferenceapp.model` & `dao`.
3. Wire controllers to load data via DAOs.
4. Replace sample CSV files in `data/` with full datasets.

---

Generated on 2025-10-20 15:19:56