// The deterministic simulation: pure Java, deliberately without any libGDX dependency.
plugins {
    id("vanguard.java-conventions")
    `java-library`
    // Allocations, the allocation measurement the sim's and the content's tests share.
    `java-test-fixtures`
}
