**This is the FTC version of the library, meaning this uses java 8 and has instructions for installing this to an FTC project. For the normal version go [here](https://github.com/omzz15/beans)**

# Java Beans

Inspired by the dependency injection in Java Spring Boot, this is a standalone library that aims to provide similar functionality to it while being easier to use. This library uses annotations to automatically create and load beans. This is useful for managing dependencies and easily switching versions using profiles.

## What does it do?
- Stores all beans in one spot for easy access
- Allows for automatic dependency injection where beans will be loaded automatically when needed
- Allows for easy switching between versions using profiles (ex: switching between debug and production versions)

## Future Additions
- More examples
- Better dependency injection
- Community Requests (your input is always important)

# How To Install
There are multiple ways to use this library, but it was primarily made for Maven/Gradle so that will be the most up to date. There will also be releases on GitHub.

## To Install with Gradle
1. Go to you `build.gradle` file
2. Add maven central to the repositories section
    ```
    repositories {
        mavenCentral()
    }
    ```
3. Add the library to the dependencies section:
    ```
    dependencies {
        implementation 'io.github.omzz15:beans:2.2.0-FTCRELEASE'
    }
    ```
4. Enjoy :)

## To Install with Maven:
1. In your project, make sure you can get libraries from Maven Central (This should be automatically available in maven projects)
2. Add the library to the project(by default, it should be in the dependencies section of pom.xml)
   ```
   <dependency>
      <groupId>io.github.omzz15</groupId>
      <artifactId>beans</artifactId>
      <version>2.2.0-FTCRELEASE</version>
   </dependency>
   ```
3. Enjoy :)

# How To Use
Check examples [here](./src/test/java/examples)
