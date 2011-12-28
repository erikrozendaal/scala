import collection.immutable._
import org.scalacheck._
import Prop._
import Gen._
import Arbitrary._
import util._
import Buildable._

object Test extends Properties("TreeMap") {
  implicit def arbTreeMap[A : Arbitrary : Ordering, B : Arbitrary]: Arbitrary[TreeMap[A, B]] =
    Arbitrary(for {
      keys <- listOf(arbitrary[A])
      values <- listOfN(keys.size, arbitrary[B])
    } yield TreeMap(keys zip values: _*))

  property("foreach/iterator consistency") = forAll { (subject: TreeMap[Int, String]) =>
    val it = subject.iterator
    var consistent = true
    subject.foreach { element =>
      consistent &&= it.hasNext && element == it.next
    }
    consistent
  }

  property("sorted") = forAll { (subject: TreeMap[Int, String]) => (subject.size >= 3) ==> {
    subject.zip(subject.tail).forall { case (x, y) => x._1 < y._1 }
  }}

  property("contains all") = forAll { (arr: List[(Int, String)]) =>
    val subject = TreeMap(arr: _*)
    arr.map(_._1).forall(subject.contains(_))
  }

  property("size") = forAll { (elements: List[(Int, Int)]) =>
    val subject = TreeMap(elements: _*)
    elements.map(_._1).distinct.size == subject.size
  }

  property("toSeq") = forAll { (elements: List[(Int, Int)]) =>
    val subject = TreeMap(elements: _*)
    elements.map(_._1).distinct.sorted == subject.toSeq.map(_._1)
  }

  property("head") = forAll { (elements: List[Int]) => elements.nonEmpty ==> {
    val subject = TreeMap(elements zip elements: _*)
    elements.min == subject.head._1
  }}

  property("last") = forAll { (elements: List[Int]) => elements.nonEmpty ==> {
    val subject = TreeMap(elements zip elements: _*)
    elements.max == subject.last._1
  }}

  property("head/tail identity") = forAll { (subject: TreeMap[Int, String]) => subject.nonEmpty ==> {
    subject == (subject.tail + subject.head)
  }}

  property("init/last identity") = forAll { (subject: TreeMap[Int, String]) => subject.nonEmpty ==> {
    subject == (subject.init + subject.last)
  }}

  property("take") = forAll { (subject: TreeMap[Int, String]) =>
    val n = choose(0, subject.size).sample.get
    n == subject.take(n).size && subject.take(n).forall(elt => subject.get(elt._1) == Some(elt._2))
  }

  property("drop") = forAll { (subject: TreeMap[Int, String]) =>
    val n = choose(0, subject.size).sample.get
    (subject.size - n) == subject.drop(n).size && subject.drop(n).forall(elt => subject.get(elt._1) == Some(elt._2))
  }

  property("take/drop identity") = forAll { (subject: TreeMap[Int, String]) =>
    val n = choose(-1, subject.size + 1).sample.get
    subject == subject.take(n) ++ subject.drop(n)
  }

  property("splitAt") = forAll { (subject: TreeMap[Int, String]) =>
    val n = choose(-1, subject.size + 1).sample.get
    val (prefix, suffix) = subject.splitAt(n)
    prefix == subject.take(n) && suffix == subject.drop(n)
  }

  property("remove single") = forAll { (subject: TreeMap[Int, String]) => subject.nonEmpty ==> {
    val key = oneOf(subject.keys.toSeq).sample.get
    val removed = subject - key
    subject.contains(key) && !removed.contains(key) && subject.size - 1 == removed.size
  }}

  property("remove all") = forAll { (subject: TreeMap[Int, String]) =>
    val result = subject.foldLeft(subject)((acc, elt) => acc - elt._1)
    result.isEmpty
  }
}
