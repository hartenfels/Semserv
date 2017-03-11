package semserv;

import scala.collection.mutable.HashMap
import scala.collection.immutable.{Vector => V};
import spray.json.{deserializationError, JsonParser, JsObject, JsValue}
import spray.json.{JsArray => JA, JsBoolean => JB, JsString  => JS}

import org.semanticweb.owlapi.model.{
  OWLNamedIndividual          => Nominal,
  OWLObjectPropertyExpression => Role,
  OWLClassExpression          => Concept
}


object interpret {
  private val cache = new HashMap[String, String] {
    override def default(source: String): String = {
      val response = interpret.transform(source).compactPrint
      this(source) = response
      response
    }
  }

  def apply(line: String): String =
    if (line.trim.isEmpty()) "" else cache(line) + "\n"


  private def transform(source: String): JsValue =
    try {
      execute(JsonParser(source))
    } catch {
      case e: Exception => JsObject(Map("error" -> JS(e.toString())))
    }

  private def execute(value: JsValue): JsValue =
    value match {
      case JA(Vector(JS(kb), JS(op), args)) =>
        new Interpreter(KnowBase(kb)).onOp(op, args)
      case _ => deserializationError("bad root")
    }


  private class Interpreter(private val kb: KnowBase) {
    def onOp(value: String, args: JsValue): JsValue =
      value match {
        case "nominal"     => JS(kb.id(onNominal(args)))
        case "satisfiable" => onSatisfiable(args)
        case "comparable"  => onComparable(args)
        case "same"        => onSame(args)
        case "query"       => onQuery(args)
        case "project"     => onProject(args)
        case "subtype"     => onSubtype(args)
        case "member"      => onMember(args)
        case _             => deserializationError("bad op")
      }

    def ja(arr: Array[Nominal]): JsValue =
      JA(arr.map(n => JS(kb.id(n))).to[Vector])

    def onNominal(value: JsValue): Nominal =
      value match {
        case JS(s) => kb.nominal(s)
        case _     => deserializationError("bad nominal")
      }

    def onSatisfiable(value: JsValue): JsValue =
      JB(kb.satisfiable(onConcept(value)))

    def onComparable(value: JsValue): JsValue =
      JB(kb.comparable(onConcept(value)))

    def onSame(value: JsValue): JsValue =
      value match {
        case JA(V(n, m)) => JB(kb.same(onNominal(n), onNominal(m)))
        case _           => deserializationError("bad same")
      }

    def onQuery(value: JsValue): JsValue =
      ja(kb.query(onConcept(value)))

    def onProject(value: JsValue): JsValue =
      value match {
        case JA(V(n, r)) => ja(kb.project(onNominal(n), onRole(r)))
        case _           => deserializationError("bad project")
      }

    def onSubtype(value: JsValue): JsValue =
      value match {
        case JA(V(c, d)) => JB(kb.subtype(onConcept(c), onConcept(d)))
        case _           => deserializationError("bad subtype")
      }

    def onMember(value: JsValue): JsValue =
      value match {
        case JA(V(c, n)) => JB(kb.member(onConcept(c), onNominal(n)))
        case _           => deserializationError("bad subtype")
      }

    def onRole(value: JsValue): Role =
      value match {
        case JA(V(JS("r"), JS(s))) => kb.role(s)
        case JA(V(JS("i"), v))     => kb.invert(onRole(v))
        case _                     => deserializationError("bad role")
      }

    def onConcept(value: JsValue): Concept =
      value match {
        case JB(true)              => kb.everything
        case JB(false)             => kb.nothing
        case JA(V(JS("C"), JS(s))) => kb.concept(s)
        case JA(V(JS("U"), JA(a))) => kb.unify(    a.map(onConcept(_)):_*)
        case JA(V(JS("I"), JA(a))) => kb.intersect(a.map(onConcept(_)):_*)
        case JA(V(JS("N"), c))     => kb.negate(onConcept(c))
        case JA(V(JS("E"), r, c))  => kb.exists(onRole(r), onConcept(c))
        case JA(V(JS("A"), r, c))  => kb.forall(onRole(r), onConcept(c))
        case _                     => deserializationError("bad concept")
      }
  }
}
