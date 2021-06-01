package shared

import scala.collection.mutable.HashMap

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
    case x: String => "s" + x.toString()
    case x: HashMap[_, _] => "m"
    case _: Object => throw MarshalExceptionUnableToSerialize(obj)
  }

  def parse(str: String): Any = {
    val identifier = str(0)
    val value = str.tail
    identifier match {
      case i if i == 'i' => value.toInt
      case i if i == 'f' => value.toFloat
      case i if i == 's' => value
      case i if i == 'h' => "bob"
      case _             => throw MarshalExceptionUnableToParse(str)
    }
  }
}
