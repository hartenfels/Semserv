package semserv;

import scala.collection.mutable.HashMap

import play.api.libs.json.{
  Json, JsValue, JsSuccess, JsError,
  JsArray   => JA,
  JsBoolean => JB,
  JsObject  => JO,
  JsString  => JS
}

import com.eclipsesource.schema._

import org.semanticweb.owlapi.model.{
  OWLNamedIndividual          => Individual,
  OWLObjectPropertyExpression => Role,
  OWLClassExpression          => Concept
}


object interpret {
  private val validator = new SchemaValidator()
  private val schema    = {
    val resource = getClass.getResourceAsStream("/semserv.schema.json")
    Json.fromJson[SchemaType](Json.parse(resource)).get
  }

  private val cache = new HashMap[String, String] {
    override def default(source: String): String = {
      // OWL API isn't thread-safe
      synchronized {
        val response = Json.stringify(interpret.transform(source))
        this(source) = response
        response
      }
    }
  }

  def apply(line: String): String =
    if (line.trim.isEmpty()) "" else cache(line) + "\n"


  private def transform(source: String): JsValue =
    try {
      val value = Json.parse(source)
      validator.validate(schema, value) match {
        case JsSuccess(_, _) => execute(value)
        case JsError(e)      => JO(Map("error" -> e.toJson))
      }
    } catch {
      case e: Exception => JO(Map("error" -> JS(e.toString)))
    }

  private def execute(value: JsValue): JsValue =
    value match {
      case JA(Seq(JS(kb), JS(op), args)) =>
        new Interpreter(KnowBase(kb)).onOp(op, args)
      case _ => throw new Exception("bad root")
    }


  private class Interpreter(private val kb: KnowBase) {
    def onOp(value: String, args: JsValue): JsValue =
      value match {
        case "individual"  => JS(kb.id(onIndividual(args)))
        case "satisfiable" => onSatisfiable(args)
        case "comparable"  => onComparable(args)
        case "same"        => onSame(args)
        case "query"       => onQuery(args)
        case "project"     => onProject(args)
        case "subtype"     => onSubtype(args)
        case "member"      => onMember(args)
        case _             => throw new Exception("bad op")
      }

    def ja(arr: Array[Individual]): JsValue =
      JA(arr.map(i => JS(kb.id(i))).to[Seq])

    def onIndividual(value: JsValue): Individual =
      value match {
        case JS(s) => kb.individual(s)
        case _     => throw new Exception("bad individual")
      }

    def onSatisfiable(value: JsValue): JsValue =
      JB(kb.satisfiable(onConcept(value)))

    def onComparable(value: JsValue): JsValue =
      value match {
        case JA(a) => JB(kb.comparable(a.map(onConcept(_)):_*))
        case _     => throw new Exception("bad comparable")
      }

    def onSame(value: JsValue): JsValue =
      value match {
        case JA(Seq(i, j)) => JB(kb.same(onIndividual(i), onIndividual(j)))
        case _             => throw new Exception("bad same")
      }

    def onQuery(value: JsValue): JsValue =
      ja(kb.query(onConcept(value)))

    def onProject(value: JsValue): JsValue =
      value match {
        case JA(Seq(i, r)) => ja(kb.project(onIndividual(i), onRole(r)))
        case _             => throw new Exception("bad project")
      }

    def onSubtype(value: JsValue): JsValue =
      value match {
        case JA(Seq(c, d)) => JB(kb.subtype(onConcept(c), onConcept(d)))
        case _             => throw new Exception("bad subtype")
      }

    def onMember(value: JsValue): JsValue =
      value match {
        case JA(Seq(c, i)) => JB(kb.member(onConcept(c), onIndividual(i)))
        case _             => throw new Exception("bad subtype")
      }

    def onRole(value: JsValue): Role =
      value match {
        case JA(Seq(JS("r"), JS(s))) => kb.role(s)
        case JA(Seq(JS("i"), v))     => kb.invert(onRole(v))
        case _                       => throw new Exception("bad role")
      }

    def onConcept(value: JsValue): Concept =
      value match {
        case JB(true)                => kb.everything
        case JB(false)               => kb.nothing
        case JA(Seq(JS("C"), JS(s))) => kb.concept(s)
        case JA(Seq(JS("O"), JS(s))) => kb.one(s)
        case JA(Seq(JS("U"), JA(a))) => kb.unify(    a.map(onConcept(_)):_*)
        case JA(Seq(JS("I"), JA(a))) => kb.intersect(a.map(onConcept(_)):_*)
        case JA(Seq(JS("N"), c))     => kb.negate(onConcept(c))
        case JA(Seq(JS("E"), r, c))  => kb.exists(onRole(r), onConcept(c))
        case JA(Seq(JS("A"), r, c))  => kb.forall(onRole(r), onConcept(c))
        case _                       => throw new Exception("bad concept")
      }
  }
}
