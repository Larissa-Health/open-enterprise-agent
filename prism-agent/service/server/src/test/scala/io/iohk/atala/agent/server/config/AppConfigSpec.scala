package io.iohk.atala.agent.server.config

import io.iohk.atala.agent.server.SystemModule
import monocle.syntax.all.*
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault

object AppConfigSpec extends ZIOSpecDefault {

  private val baseVaultConfig = VaultConfig(
    address = "http://localhost:8200",
    token = None,
    appRoleRoleId = None,
    appRoleSecretId = None,
    useSemanticPath = true,
  )

  override def spec = suite("AppConfigSpec")(
    test("load config successfully") {
      for {
        appConfig <- ZIO.serviceWith[AppConfig](
          _.focus(_.agent.secretStorage.backend)
            .replace(SecretStorageBackend.memory)
        )
      } yield assert(appConfig.validate)(isRight(anything))
    },
    test("reject config when use vault secret storage and config is empty") {
      for {
        appConfig <- ZIO.serviceWith[AppConfig](
          _.focus(_.agent.secretStorage.backend)
            .replace(SecretStorageBackend.vault)
            .focus(_.agent.secretStorage.vault)
            .replace(None)
        )
      } yield assert(appConfig.validate)(isLeft(containsString("config is not provided")))
    },
    test("reject config when use vault secret storage and authentication is not provided") {
      for {
        appConfig <- ZIO.serviceWith[AppConfig](
          _.focus(_.agent.secretStorage.backend)
            .replace(SecretStorageBackend.vault)
            .focus(_.agent.secretStorage.vault)
            .replace(Some(baseVaultConfig))
        )
      } yield assert(appConfig.validate)(isLeft(containsString("authentication must be provided")))
    },
    test("load config when use vault secret storage with token authentication") {
      for {
        appConfig <- ZIO.serviceWith[AppConfig](
          _.focus(_.agent.secretStorage.backend)
            .replace(SecretStorageBackend.vault)
            .focus(_.agent.secretStorage.vault)
            .replace(Some(baseVaultConfig.copy(token = Some("token"))))
        )
      } yield assert(appConfig.validate)(isRight(anything))
    },
    test("load config when use vault secret storage with appRole authentication") {
      for {
        appConfig <- ZIO.serviceWith[AppConfig](
          _.focus(_.agent.secretStorage.backend)
            .replace(SecretStorageBackend.vault)
            .focus(_.agent.secretStorage.vault)
            .replace(Some(baseVaultConfig.copy(appRoleRoleId = Some("roleId"), appRoleSecretId = Some("secretId"))))
        )
      } yield assert(appConfig.validate)(isRight(anything))
    },
    test("prefer vault token authentication when multiple auth methods are provided") {
      val vaultConfig = baseVaultConfig.copy(
        token = Some("token"),
        appRoleRoleId = Some("roleId"),
        appRoleSecretId = Some("secretId"),
      )
      assert(vaultConfig.validate)(isRight(isSubtype[ValidatedVaultConfig.TokenAuth](anything)))
    }
  ).provide(SystemModule.configLayer)

}
