/*
 * Copyright 2017 Carsten Hartenfels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package semserv

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


class DummyCache() {
  def get(req: String): Option[String] = None
  def set(req: String, res: String): String = res
}

object DummyCache {
  def apply(): DummyCache = new DummyCache()
}


object interpret {
  private val validator = new SchemaValidator()
  private val schema    = {
    val resource = getClass.getResourceAsStream("/semserv.schema.json")
    Json.fromJson[SchemaType](Json.parse(resource)).get
  }


  private val cache = DummyCache() // RequestCache()

  private def respond(req: String): String =
    cache.get(req).getOrElse(
      cache.set(req, Json.stringify(interpret.transform(req))))

  def apply(line: String): String =
    if (line.trim.isEmpty()) "" else respond(line) + "\n"


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
      case JA(Seq(JS(kb), JS(op), args)) => synchronized {
        // OWL API isn't thread-safe
        new Interpreter(KnowBase(kb)).onOp(op, args)
      }
      case _ => throw new Exception("bad root")
    }


  private class Interpreter(private val kb: KnowBase) {
    def onOp(value: String, args: JsValue): JsValue =
      value match {
        case "individual"     => JS(kb.id(onIndividual(args)))
        case "satisfiable"    => onSatisfiable(args)
        case "same"           => onSame(args)
        case "query"          => onQuery(args)
        case "project"        => onProject(args)
        case "subtype"        => onSubtype(args)
        case "member"         => onMember(args)
        case "signature"      => onSignature(args)
        case "addIndividual!" => onAddIndividual(args)
        case "addTriple!"     => onAddTriple(args)
        case _                => throw new Exception("bad op")
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
        case JB(true)                => kb.topRole
        case JB(false)               => kb.bottomRole
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

    def onSignature(value: JsValue): JsValue =
      value match {
        case JA(Seq(JS("concept"),    JS(iri))) => JB(kb.hasConceptInSignature(iri))
        case JA(Seq(JS("role"),       JS(iri))) => JB(kb.hasRoleInSignature(iri))
        case JA(Seq(JS("individual"), JS(iri))) => JB(kb.hasIndividualInSignature(iri))
        case _                                  => throw new Exception("bad signature")
      }

    def onAddIndividual(value: JsValue): JsValue =
      value match {
        case JA(Seq(i, c)) => JB(kb.addIndividual(onIndividual(i), onConcept(c)))
        case _             => throw new Exception("bad addIndividual")
      }

    def onAddTriple(value: JsValue): JsValue =
      value match {
        case JA(Seq(i, r, j)) =>
          JB(kb.addTriple(onIndividual(i), onRole(r), onIndividual(j)))
        case _ =>
          throw new Exception("bad addTriple")
      }
  }
}
