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

import java.io.File
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.reasoner.NodeSet
import scala.collection.mutable.HashMap
import scala.util.Try

import org.semanticweb.owlapi.model.{
  IRI, OWLAxiom, OWLDataFactory, OWLLiteral, OWLOntology,
  OWLNamedIndividual          => Individual,
  OWLObjectPropertyExpression => Role,
  OWLClassExpression          => Concept,
  OWLDataProperty             => Property
}


object KnowBase {
  private val mgr = OWLManager.createOWLOntologyManager()
  private val df  = mgr.getOWLDataFactory

  private val cache = new HashMap[String, KnowBase] {
    override def default(path: String): KnowBase = {
      val onto   = mgr.loadOntologyFromOntologyDocument(new File(path))
      val kb     = new KnowBase(df, onto)
      this(path) = kb
      kb
    }
  }

  def apply(path: String): KnowBase =
    cache(new File(path).getAbsolutePath)
}


class KnowBase(df: OWLDataFactory, onto: OWLOntology) {
  private val hermit = new Reasoner(onto)
  private val pre    = hermit.getPrefixes

  hermit.precomputeInferences()


  private def toIRI (s: String): IRI =
    IRI.create(Try(pre.expandAbbreviatedIRI(s)) getOrElse s)

  def individual(s: String): Individual = df.getOWLNamedIndividual(toIRI(s))
  def role      (s: String): Role       = df.getOWLObjectProperty(toIRI(s))
  def concept   (s: String): Concept    = df.getOWLClass(toIRI(s))
  def property  (s: String): Property   = df.getOWLDataProperty(toIRI(s))

  def id(i: Individual): String = i.toStringID


  def hasConceptInSignature(s: String): Boolean =
    onto.containsClassInSignature(toIRI(s))

  def hasRoleInSignature(s: String): Boolean =
    onto.containsObjectPropertyInSignature(toIRI(s))

  def hasIndividualInSignature(s: String): Boolean =
    onto.containsIndividualInSignature(toIRI(s))

  def hasPropertyInSignature(s: String): Boolean =
    onto.containsDataPropertyInSignature(toIRI(s))


  def topRole:    Role = df.getOWLTopObjectProperty
  def bottomRole: Role = df.getOWLBottomObjectProperty

  def invert(r: Role): Role = df.getOWLObjectInverseOf(r)

  def everything: Concept = df.getOWLThing
  def nothing:    Concept = df.getOWLNothing

  def one(s: String): Concept = df.getOWLObjectOneOf(individual(s))

  def unify    (cs: Concept*): Concept = df.getOWLObjectUnionOf(cs:_*)
  def intersect(cs: Concept*): Concept = df.getOWLObjectIntersectionOf(cs:_*)
  def negate   (c:  Concept ): Concept = df.getOWLObjectComplementOf(c)

  def exists(r: Role, c: Concept): Concept = df.getOWLObjectSomeValuesFrom(r, c)
  def forall(r: Role, c: Concept): Concept = df.getOWLObjectAllValuesFrom(r, c)


  def satisfiable(c: Concept): Boolean = hermit.isSatisfiable(c)

  def same(i: Individual, j:Individual): Boolean = hermit.isSameIndividual(i, j)


  private def flat(set: NodeSet[Individual]): Array[Individual] =
    set.getFlattened().toArray(new Array[Individual](0))

  def query(c: Concept): Array[Individual] =
    flat(hermit.getInstances(c, false))

  def project(i: Individual, r: Role): Array[Individual] =
    flat(hermit.getObjectPropertyValues(i, r))

  def appropriate(i: Individual, p: Property): Array[OWLLiteral] =
    hermit.getDataPropertyValues(i, p).toArray(new Array[OWLLiteral](0))


  private def entail(a: OWLAxiom, b: OWLAxiom): Boolean =
    hermit.isEntailed(a) && !hermit.isEntailed(b)

  def subtype(c: Concept, d: Concept): Boolean =
    entail(
      df.getOWLSubClassOfAxiom(c, d),
      df.getOWLSubClassOfAxiom(c, df.getOWLObjectComplementOf(d)))

  def member(c: Concept, i: Individual): Boolean =
    entail(
      df.getOWLClassAssertionAxiom(c, i),
      df.getOWLClassAssertionAxiom(df.getOWLObjectComplementOf(c), i))
}
