package com.github.kitakkun.aspectk.compiler.test

import com.github.kitakkun.aspectk.compiler.test.runners.AbstractAspectKBoxTest
import com.github.kitakkun.aspectk.compiler.test.runners.AbstractAspectKDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "aspectk-compiler/testData", testsRoot = "aspectk-compiler/test-gen") {
            testClass<AbstractAspectKDiagnosticTest> {
                model("diagnostics")
            }
            testClass<AbstractAspectKBoxTest> {
                model("box")
            }
        }
    }
}
