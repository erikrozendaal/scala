/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 */

package scala.tools.nsc
package interpreter

import scala.tools.nsc.io.AbstractFile
import util.ScalaClassLoader
import java.net.{ URL, URLConnection, URLStreamHandler }
import scala.collection.{ mutable, immutable }

/**
 * A class loader that loads files from a {@link scala.tools.nsc.io.AbstractFile}.
 *
 * @author Lex Spoon
 */
class AbstractFileClassLoader(val root: AbstractFile, parent: ClassLoader)
    extends ClassLoader(parent)
    with ScalaClassLoader
{
  protected def classNameToPath(name: String): String =
    if (name endsWith ".class") name
    else name.replace('.', '/') + ".class"

  protected def findAbstractFile(name: String): AbstractFile = {
    var file: AbstractFile = root
    val pathParts          = name split '/'

    for (dirPart <- pathParts.init) {
      file = file.lookupName(dirPart, directory = true)
      if (file == null)
        return null
    }

    file.lookupName(pathParts.last, directory = false) match {
      case null   => null
      case file   => file
    }
  }

  protected def dirNameToPath(name: String): String =
    name.replace('.', '/')

  protected def findAbstractDir(name: String): AbstractFile = {
    var file: AbstractFile = root
    val pathParts          = dirNameToPath(name) split '/'

    for (dirPart <- pathParts) {
      file = file.lookupName(dirPart, directory = true)
      if (file == null)
        return null
    }

    file
  }

  // parent delegation in JCL uses getResource; so either add parent.getResAsStream
  // or implement findResource, which we do here as a study in scarlet (my complexion
  // after looking at CLs and URLs)
  override def findResource(name: String): URL = findAbstractFile(name) match {
    case null => null
    case file => new URL(null, "repldir:" + file.path, new URLStreamHandler {
      override def openConnection(url: URL): URLConnection = new URLConnection(url) {
        override def connect() { }
        override def getInputStream = file.input
      }
    })
  }

  // this inverts delegation order: super.getResAsStr calls parent.getRes if we fail
  override def getResourceAsStream(name: String) = findAbstractFile(name) match {
    case null => super.getResourceAsStream(name)
    case file => file.input
  }
  // ScalaClassLoader.classBytes uses getResAsStream, so we'll try again before delegating
  override def classBytes(name: String): Array[Byte] = findAbstractFile(classNameToPath(name)) match {
    case null => super.classBytes(name)
    case file => file.toByteArray
  }
  override def findClass(name: String): JClass = {
    val bytes = classBytes(name)
    if (bytes.length == 0)
      throw new ClassNotFoundException(name)
    else
      defineClass(name, bytes, 0, bytes.length)
  }

  private val packages = mutable.Map[String, Package]()

  override def definePackage(name: String, specTitle: String, specVersion: String, specVendor: String, implTitle: String, implVersion: String, implVendor: String, sealBase: URL): Package = {
    throw new UnsupportedOperationException()
  }

  override def getPackage(name: String): Package = {
    findAbstractDir(name) match {
      case null => super.getPackage(name)
      case file => packages.getOrElseUpdate(name, {
        val ctor = classOf[Package].getDeclaredConstructor(classOf[String], classOf[String], classOf[String], classOf[String], classOf[String], classOf[String], classOf[String], classOf[URL], classOf[ClassLoader])
        ctor.setAccessible(true)
        ctor.newInstance(name, null, null, null, null, null, null, null, this)
      })
    }
  }

  override def getPackages(): Array[Package] =
    root.iterator.filter(_.isDirectory).map(dir => getPackage(dir.name)).toArray
}
