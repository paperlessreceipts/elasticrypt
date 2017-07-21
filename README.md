# elasticrypt

This plugin attempts to provide tenanted encryption at rest in Elasticsearch. Below is a brief overview of the files in the plugin and how they are used.

## Plugin

**ElasticryptPlugin.scala**
Entry point for the plugin. Defines plugin name and description.


## Low-Level Encrypted I/O

**AESReader.java**
Core decryption class that uses AES 256-bit ciphers to decrypt a given file. Adapted from https://issues.apache.org/jira/browse/LUCENE-2228

**AESWriter.java**
Core encryption class that uses AES 256-bit ciphers to encrypt a given file. Adapted from https://issues.apache.org/jira/browse/LUCENE-2228

**FileHeader.scala**
Interface for writing unencrypted metadata at the beginning of an encrypted file.

**HmacFileHeader.scala**
Implementation of the `FileHeader` interface that adds a MAC hash that is used to verify that the correct key is being used to decrypt a file.

**HmacUtil.scala**
Utility functions used in the `HmacFileHeader` class.

**EncryptedFileChannel.scala**
Extension of `java.nio.channels.FileChannel` that instantiates an AESReader and AESWriter to encrypt all reads and writes. Utilized in `EncryptedRafReference` and `EncryptedTranslogStream`.

**EncryptedRafReference.scala**
Extends `org.elasticsearch.index.translog.fs.RafReference` and overrides the `channel()` method to return an `EncryptedFileChannel`.


## Key Management

**KeyProviderFactory.scala**
A singleton object that acts as a factory for key providers. Includes the KeyProvider trait, an outline for a basic key provider.

**HardcodedKeyProvider.scala**
Dummy implementation of the `KeyProvider` trait as a proof of concept. We plan to introduce more complex KeyProviders such as `HttpKeyProvider` in a followup PR.

**EncryptedNodeModule.scala**
An `org.elasticsearch.common.inject.AbstractModule` that enables injection of `NodeKeyProviderComponent`.

**NodeKeyProviderComponent.scala**
Defines the `KeyProvider` to be used by this node.


## Translog Encryption

**EncryptedTranslog.scala**
Extends `org.elasticsearch.index.translog.fs.FsTranslog` and overrides `createRafReference()` and `translogStreamFor()` to return an `EncryptedRafReference` and `EncryptedTranslogStream` respectively. Both `createRafReference()` and `translogStreamFor()` are small methods that we added to `FsTranslog` so that they could be overriden here.

**EncryptedTranslogStream.scala**
Extension of `org.elasticsearch.index.translog.ChecksummedTranslogStream` that overrides `openInput()` to use a `ChannelInputStream` that wraps an  `EncryptedFileChannel`.


## Lucene Directory-Level Encryption

**EncryptedDirectory.scala**
This class extends `org.apache.lucene.store.NIOFSDirectory` and overrides `createOutput()` and `openInput()` to include encryption and decryption via `AESIndexOutput` and `AESIndexInput` respectively. Code is based on the existing implementation in NIOFSDirectory:
 - https://github.com/apache/lucene-solr/blob/master/lucene/core/src/java/org/apache/lucene/store/NIOFSDirectory.java
 - https://www.elastic.co/guide/en/elasticsearch/reference/1.7/index-modules-store.html#default_fs

 **AESIndexOutput.scala**
Class that extends `org.apache.lucene.store.OutputStreamIndexOutput`, using `AESChunkedOutputStreamBuilder` to build a `ChunkedOutputStream` that wraps an `AESWriterOutputStream`.

**AESIndexInput.scala**
Extension of `org.apache.lucene.store.BufferedIndexInput` that uses an instance of `AESReader` to perform reads on encrypted files. Utilized in `EncryptedDirectory` on `openInput()`.

**AESChunkedOutputStreamBuilder.scala**
Builder that creates a `ChunkedOutputStream` that wraps an `AESWriterOutputStream`.

**ChunkedOutputStream.scala**
This code is based on the existing implementation in FSDirectory:
 - https://github.com/apache/lucene-solr/blob/master/lucene/core/src/java/org/apache/lucene/store/FSDirectory.java#L412

**AESWriterOutputStream.scala**
Extension of `java.io.OutputStream` that wraps an AESWriter and routes all writes through it.

**EncryptedDirectoryService.scala**
Class that extends `org.elasticsearch.index.store.fs.FsDirectoryService` and overrides `newFSDirectory()` to return an `EncryptedDirectory`.

**EncryptedIndexStore.scala**
Extension of `org.elasticsearch.index.store.fs.FsIndexStore` that overrides `shardDirectory()` to return the class of `EncryptedDirectoryService`.

**EncryptedIndexStoreModule.scala**
An `org.elasticsearch.common.inject.AbstractModule` that enables injection of `EncryptedIndexStore`.

