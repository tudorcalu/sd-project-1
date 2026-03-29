# Architecture: Local File Search Engine

## 1. System Context (Level 1)
The system context is the top level that illustrates the entire system as a whole and the complex way it interacts with both the human user and the adjacent operating environment.

* **User:** A desktop computer user who interacts directly with the application's graphical interface to quickly and efficiently locate the various files stored on their local drives.
* **System (Local Search Engine):** The core application developed in Java that has the vital role of indexing the files on the current device, including documents, media files, and binary files, with the aim of providing an extremely fast search experience where results appear fluidly as the user types.

## 2. Containers (Level 2)
The system context is made up of a series of containers that function as distinct deployable units, thus facilitating the logical organization of the entire architectural ensemble.

* **Desktop UI Application (Java / JavaFX):** This is the main graphical interface that takes the data entered by the user and which must feel fast and responsive, displaying results immediately and including those file previews that are absolutely crucial for the satisfaction of the end user.
* **Background Indexing Service (Java ExecutorService):** A continuous and invisible process for the user that traverses local content, filters out unwanted data, and stores the necessary information in a database, focusing exclusively in this first iteration on searching for content within text files.
* **Relational Database (H2 Database` Engine):** A local embedded storage solution, managed by the preferred database management system, which will determine exactly how the data is processed at the time of query and which takes on the complex task of formatting the built index.

## 3. Components (Level 3)
Each container is made up of several interconnected components, these representing the major structural blocks in the code that ensure the specific and robust functionality of the system.

* **File Crawler:** A core component built with Java NIO that recursively traverses the file system and must be designed to gracefully and without crashing handle all those unforeseen edge cases, such as file permission issues, infinite symbolic link loops, or database connection timeouts.
* **Content and Metadata Extractor:** An entity responsible for extracting every bit of information deemed important, including preserving all available metadata, such as file extensions, tags, or timestamps, using libraries like Apache Tika to easily support future use cases.
* **Search and Preview Controller:** An intelligent module that translates user queries into appropriate JDBC database commands to allow single-word and multi-word searches to function properly, while returning useful contextual snippets, such as the first three lines of text of a found document.

## 4. Code (Level 4)
Finally, at the most detailed level of the model, each component presented above is made up of a multitude of specific classes that contain a complex set of lower-level methods or functions, implemented directly in the Java source code.

* **IndexBuilder.java:** The central class that closely monitors the progress of the file indexing process and is responsible for generating a detailed report at the end of execution, thus respecting the specific requirements for obtaining a higher-level functionality.
* **DatabaseContext.java and QueryHandler.java:** Structural classes that define the database schema using perfectly adapted data types and indexes, and that formulate well-written and highly efficient SQL queries, elements that are absolutely essential for obtaining accurate search results.
