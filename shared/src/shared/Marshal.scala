package shared

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer

object Marshal {
  private case class MarshalExceptionUnableToSerialize(obj: Any)
      extends Exception
  private case class MarshalExceptionUnableToParse(input: String)
      extends Exception

  def serialize(obj: Any): String = obj match {
    case x: Int => "i" + x.toString()
    case x: Float =>
      "f" + BigDecimal(x)
        .setScale(5, BigDecimal.RoundingMode.HALF_UP)
        .toString()
    case x: String    => "s" + x.toString()
    case x: Map[_, _] => "m" + serializeMap(x)
    case _: Object    => throw MarshalExceptionUnableToSerialize(obj)
  }

  def parse(str: String): Any = {
    val identifier = str(0)
    val value = str.tail
    identifier match {
      case i if i == 'i' => value.toInt
      case i if i == 'f' => value.toFloat
      case i if i == 's' => value
      case i if i == 'm' => parseMap(value)
      case _             => throw MarshalExceptionUnableToParse(str)
    }
  }

  private def serializeMap(map: Map[_, _]): String = {
    val pairs = new ArrayBuffer[String]()
    map.foreachEntry((key, value) =>
      pairs.addOne(f"${key}~${serialize(value)}")
    )
    return pairs.mkString(",")
  }

  private def parseMap(str: String): Map[_, _] = {
    val map = new HashMap[Any, Any]()
    val pairs = str.split(',')
    pairs.foreach(pair => {
      val segments = pair.split('~')
      val key = segments(0)
      val value = parse(segments(1))
      map.put(key, value)
    })
    return map
  }
}
