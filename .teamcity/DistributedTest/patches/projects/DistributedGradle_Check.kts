package DistributedTest.patches.projects

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the project with uuid = 'DistributedGradle_Check' (id = 'DistributedTest')
accordingly, and delete the patch script.
*/
changeProject(uuid("DistributedGradle_Check")) {
    params {
        expect {
            param("env.ARTIFACTORY_PASSWORD", "%artifactoryPassword%")
        }
        update {
            param("env.ARTIFACTORY_PASSWORD", "bot-enterprise-releases")
        }
        expect {
            param("env.GRADLE_ENTERPRISE_ACCESS_KEY", "%e.grdev.net.access.key%")
        }
        update {
            param("env.GRADLE_ENTERPRISE_ACCESS_KEY", "credentialsJSON:3dadbc2c-a4ef-49ff-a9d2-ed682a4e26ac")
        }
    }
}
