package com.workday.elasticrypt

import java.nio.file.{Files, Paths}
import javax.crypto.spec.SecretKeySpec
import scala.collection.JavaConverters._

object Keyring {
    private[this] val keyring = Paths.get(
        sys.env.getOrElse("ELASTICRYPT_FILE_KEYRING",
            sys.env.getOrElse("CONF_DIR", "/etc/elasticsearch") + "/keyring"))

    val key = {
        if (Files.isDirectory(keyring)) {
            val keys = (
                    Files.list(keyring).iterator().asScala map {
                        path => (path.getFileName().toString(), Files.readAllBytes(path))
                    } toMap
                )
            (indexName: String) => keys(indexName)
        } else {
            val key = Files.readAllBytes(keyring)
            (indexName: String) => key
        }
    }
}

class FileKeyProvider() extends KeyProvider {
  val ALGORITHM_AES = "AES"

  def getKey(indexName: String): SecretKeySpec = {
    new SecretKeySpec(Keyring.key(indexName), ALGORITHM_AES)
  }
}
