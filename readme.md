### Getting Started

##### Prerequisites
 - Scala
 - Mill
 - Node

```
brew install scala
brew install mill
```

It is recommended to use Visual Studio Code with the Scala Metals plugin. Metals can import the build file which will allow the dependencies to be resolved correctly.

If you are using IntelliJ Idea, it is required to run `npm run idea:reload` when any changes are made to the `build.sc` file. This will rebuild IntelliJ Idea files to ensure the editor resolves dependencies correctly.
