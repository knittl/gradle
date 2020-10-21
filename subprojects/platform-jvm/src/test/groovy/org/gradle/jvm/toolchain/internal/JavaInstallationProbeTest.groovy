/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.toolchain.internal

import org.gradle.api.JavaVersion
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecActionFactory
import org.gradle.process.internal.JavaExecAction
import org.gradle.test.fixtures.file.TestFile
import org.gradle.testfixtures.internal.NativeServicesTestFixture
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.jvm.toolchain.internal.JavaInstallationProbe.InstallType.INVALID_JDK
import static org.gradle.jvm.toolchain.internal.JavaInstallationProbe.InstallType.IS_JDK
import static org.gradle.jvm.toolchain.internal.JavaInstallationProbe.InstallType.IS_JRE
import static org.gradle.jvm.toolchain.internal.JavaInstallationProbe.InstallType.NO_SUCH_DIRECTORY

class JavaInstallationProbeTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder

    @Unroll("Can probe version of #jdk is #displayName")
    def "probes java installation"() {
        given:
        def execFactory = Mock(ExecActionFactory)
        def javaExec = Mock(JavaExecAction)
        execFactory.newJavaExecAction() >> javaExec
        PrintStream output
        javaExec.setStandardOutput(_) >> {
            def outputStream = it[0]
            output = new PrintStream(outputStream)
            null
        }
        javaExec.execute() >> {
            systemProperties.each {
                output.println(it.value)
            }
            Mock(ExecResult)
        }
        def install = new JavaInstall()

        when:
        def probe = new JavaInstallationProbe(execFactory)
        File javaHome = new File(jdk)
        if (exists) {
            javaHome = temporaryFolder.newFolder(jdk)
            if (!jre) {
                def binDir = new File(javaHome, "bin")
                if (binDir.mkdir()) {
                    File javac = new File(binDir, OperatingSystem.current().getExecutableName('javac'))
                    javac << 'dummy'
                }
            }
        }
        def probeResult = probe.checkJdk(javaHome)
        if (expectedResult == IS_JDK || expectedResult == IS_JRE) {
            probeResult.configure(install)
        }

        then:
        probeResult.installType == expectedResult
        if (expectedResult == IS_JDK || expectedResult == IS_JRE) {
            assert install.javaVersion == javaVersion
            assert displayName == null || install.displayName == displayName
            assert probeResult.javaHome != null
        }

        where:
        jdk                                   | systemProperties       | javaVersion             | displayName           | exists | jre   | expectedResult
        'localGradle'                         | currentGradle()        | JavaVersion.current()   | null                  | true   | false | IS_JDK
        'localGradle'                         | currentGradle()        | JavaVersion.current()   | null                  | true   | true  | IS_JRE
        'localGradle'                         | currentGradle()        | null                    | null                  | false  | false | NO_SUCH_DIRECTORY
        'openJdk4'                            | openJdkJvm('4')        | JavaVersion.VERSION_1_4 | 'OpenJDK 4'           | true   | false | IS_JDK
        'openJdk5'                            | openJdkJvm('5')        | JavaVersion.VERSION_1_5 | 'OpenJDK 5'           | true   | false | IS_JDK
        'openJdk6'                            | openJdkJvm('6')        | JavaVersion.VERSION_1_6 | 'OpenJDK 6'           | true   | false | IS_JDK
        'openJdk7'                            | openJdkJvm('7')        | JavaVersion.VERSION_1_7 | 'OpenJDK 7'           | true   | false | IS_JDK
        'openJdk8'                            | openJdkJvm('8')        | JavaVersion.VERSION_1_8 | 'OpenJDK 8'           | true   | false | IS_JDK
        'openJdk9'                            | openJdkJvm('9')        | JavaVersion.VERSION_1_9 | 'OpenJDK 9'           | true   | false | IS_JDK
        'openJdk9'                            | openJdkJvm('9')        | JavaVersion.VERSION_1_9 | 'OpenJDK JRE 9'       | true   | true  | IS_JRE
        'AdoptOpenJDK11'                      | adoptOpenJDK('11.0.3') | JavaVersion.VERSION_11  | 'AdoptOpenJDK 11'     | true   | false | IS_JDK
        'AdoptOpenJDK11'                      | adoptOpenJDK('11.0.3') | JavaVersion.VERSION_11  | 'AdoptOpenJDK JRE 11' | true   | true  | IS_JRE
        'oracleJdk4'                          | oracleJvm('4')         | JavaVersion.VERSION_1_4 | 'Oracle JDK 4'        | true   | false | IS_JDK
        'oracleJre4'                          | oracleJvm('4')         | JavaVersion.VERSION_1_4 | 'Oracle JRE 4'        | true   | true  | IS_JRE
        'oracleJdk5'                          | oracleJvm('5')         | JavaVersion.VERSION_1_5 | 'Oracle JDK 5'        | true   | false | IS_JDK
        'oracleJdk6'                          | oracleJvm('6')   | JavaVersion.VERSION_1_6 | 'Oracle JDK 6' | true | false | IS_JDK
        'oracleJdk7'                          | oracleJvm('7')   | JavaVersion.VERSION_1_7 | 'Oracle JDK 7' | true | false | IS_JDK
        'oracleJdk8'                          | oracleJvm('8')   | JavaVersion.VERSION_1_8 | 'Oracle JDK 8' | true | false | IS_JDK
        'oracleJdk9'                          | oracleJvm('9')   | JavaVersion.VERSION_1_9 | 'Oracle JDK 9' | true | false | IS_JDK
        'oracleJre9'                          | oracleJvm('9')   | JavaVersion.VERSION_1_9 | 'Oracle JRE 9' | true | true  | IS_JRE
        'ibmJdk4'                             | ibmJvm('4')      | JavaVersion.VERSION_1_4 | 'IBM JDK 4'    | true | false | IS_JDK
        'ibmJre4'                             | ibmJvm('4')      | JavaVersion.VERSION_1_4 | 'IBM JRE 4'    | true | true  | IS_JRE
        'ibmJdk5'                             | ibmJvm('5')      | JavaVersion.VERSION_1_5 | 'IBM JDK 5'    | true | false | IS_JDK
        'ibmJdk6'                             | ibmJvm('6')      | JavaVersion.VERSION_1_6 | 'IBM JDK 6'    | true | false | IS_JDK
        'ibmJdk7'                             | ibmJvm('7')      | JavaVersion.VERSION_1_7 | 'IBM JDK 7'    | true | false | IS_JDK
        'ibmJdk8'                             | ibmJvm('8')      | JavaVersion.VERSION_1_8 | 'IBM JDK 8'    | true | false | IS_JDK
        'ibmJdk9'                             | ibmJvm('9')      | JavaVersion.VERSION_1_9 | 'IBM JDK 9'    | true | false | IS_JDK
        'zuluJre6'                            | zuluJvm('6')     | JavaVersion.VERSION_1_6 | 'Zulu JRE 6'   | true | true  | IS_JRE
        'zuluJdk8'                            | zuluJvm('8')     | JavaVersion.VERSION_1_8 | 'Zulu JDK 8'   | true | false | IS_JDK
        'hpuxJre6'                            | hpuxJvm('6')     | JavaVersion.VERSION_1_6 | 'HP-UX JRE 6'  | true | true  | IS_JRE
        'hpuxJdk7'                            | hpuxJvm('7')     | JavaVersion.VERSION_1_7 | 'HP-UX JDK 7'  | true | false | IS_JDK
        'sapjdk13'                            | sapJvm('13')      | JavaVersion.VERSION_13 | 'SAP SapMachine JDK 13'  | true | false | IS_JDK
        'sapjre13'                            | sapJvm('13')      | JavaVersion.VERSION_13 | 'SAP SapMachine JRE 13'  | true | true | IS_JRE
        'correttojdk11'                            | correttoJvm('11')      | JavaVersion.VERSION_11 | 'Amazon Corretto JDK 11'  | true | false | IS_JDK
        'correttojre11'                            | correttoJvm('11')      | JavaVersion.VERSION_11 | 'Amazon Corretto JRE 11'  | true | true | IS_JRE
        'whitespaces'                         | whitespaces('11.0.3')  | JavaVersion.VERSION_11  | 'AdoptOpenJDK JRE 11' | true   | true  | IS_JRE
        'binary that has invalid output'      | invalidOutput()  | null                    | null           | true | false | INVALID_JDK
        'binary that returns unknown version' | invalidVersion() | null                    | null           | true | false | INVALID_JDK
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "cached probe are not affected by symlink changes"() {
        given:
        NativeServicesTestFixture.initialize()
        def execFactory = Mock(ExecActionFactory)
        def probe = new JavaInstallationProbe(execFactory)
        File javaHome1 = Jvm.current().javaHome
        def link = new TestFile(temporaryFolder.newFolder(), "jdklink")
        link.createLink(javaHome1)

        when:
        def probe1 = probe.checkJdk(link)
        link.createLink(new File("doesntExist"))
        def probe2 = probe.checkJdk(link)

        then:
        probe1.javaHome.toString().contains(Jvm.current().javaHome.canonicalPath)
        probe2.error.contains("No such directory")
    }

    private static Map<String, String> invalidOutput() {
        // emulates a binary that would not return the correct number of outputs
        [:]
    }

    private static Map<String, String> invalidVersion() {
        // emulates a valid binary that would not return the correct version number
        ['java.version': "bad luck",
         'java.vendor': "Oracle Corporation",
         'os.arch': "amd64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.40-b25",
         'java.runtime.name': "Java(TM) SE Runtime Environment"
        ]
    }


    private static Map<String, String> currentGradle() {
        ['java.home', 'java.version', 'java.vendor', 'os.arch', 'java.vm.name', 'java.vm.version', 'java.runtime.name'].collectEntries { [it, System.getProperty(it)] }
    }

    private static Map<String, String> openJdkJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0",
         'java.vendor': "Oracle Corporation",
         'os.arch': "amd64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.40-b25",
         'java.runtime.name': "Java(TM) SE Runtime Environment"
        ]
    }

    private static Map<String, String> adoptOpenJDK(String version) {
        ['java.home': "java-home",
         'java.version': version,
         'java.vendor': "AdoptOpenJDK",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "${version}+7",
         'java.runtime.name': "OpenJDK Runtime Environment"
        ]
    }

    private static Map<String, String> whitespaces(String version) {
        ['java.home': "home-with-whitespaces\r",
         'java.version': version,
         'java.vendor': "AdoptOpenJDK\r",
         'os.arch': "x86_64\r",
         'java.vm.name': "OpenJDK 64-Bit Server VM\r",
         'java.vm.version': "${version}+7\r",
         'java.runtime.name': "OpenJDK Runtime Environment\r"
        ]
    }

    private static Map<String, String> oracleJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0",
         'java.vendor': "Oracle Corporation",
         'os.arch': "amd64",
         'java.vm.name': "Java HotSpot(TM) 64-Bit Server VM",
         'java.vm.version': "25.40-b25",
         'java.runtime.name': "Java(TM) SE Runtime Environment"
        ]
    }

    private static Map<String, String> ibmJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0",
         'java.vendor': "IBM Corporation",
         'os.arch': "amd64",
         'java.vm.name': "IBM J9 VM",
         'java.vm.version': "2.4",
         'java.runtime.name': "Java(TM) SE Runtime Environment"
        ]
    }

    private static Map<String, String> zuluJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0_66",
         'java.vendor': "Azul Systems, Inc.",
         'os.arch': "amd64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.66-b17",
         'java.runtime.name': "OpenJDK Runtime Environment"
        ]
    }

    private static Map<String, String> hpuxJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0_66",
         'java.vendor': "Hewlett-Packard Co.",
         'os.arch': "ia64",
         'java.vm.name': "Java HotSpot(TM) 64-Bit Server VM",
         'java.vm.version': "25.66-b17",
         'java.runtime.name': "Java(TM) SE Runtime Environment"
        ]
    }

    private static Map<String, String> sapJvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}.0.2",
         'java.vendor': "SAP SE",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "13.0.2+8-sapmachine",
         'java.runtime.name': "OpenJDK Runtime Environment"
        ]
    }

    private static Map<String, String> correttoJvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}.0.8",
         'java.vendor': "Amazon.com Inc.",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "11.0.8+10-LTS",
         'java.runtime.name': "OpenJDK Runtime Environment"
        ]
    }

    private static class JavaInstall implements InstalledJdk {
        String name
        JavaVersion javaVersion
        String displayName
        File javaHome
    }

}
