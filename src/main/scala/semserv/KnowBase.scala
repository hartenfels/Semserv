package semserv

import java.io.File
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.reasoner.NodeSet
import scala.collection.mutable.HashMap
import scala.util.Try

import org.semanticweb.owlapi.model.{
  IRI, OWLAxiom, OWLDataFactory, OWLOntology,
  OWLNamedIndividual          => Individual,
  OWLObjectPropertyExpression => Role,
  OWLClassExpression          => Concept
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

  def id(i: Individual): String = pre.abbreviateIRI(i.toStringID)


  def hasConceptInSignature(s: String): Boolean =
    onto.containsClassInSignature(toIRI(s))

  def hasRoleInSignature(s: String): Boolean =
    onto.containsObjectPropertyInSignature(toIRI(s))

  def hasIndividualInSignature(s: String): Boolean =
    onto.containsIndividualInSignature(toIRI(s))


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

  def comparable(cs: Concept*): Boolean = satisfiable(intersect(cs:_*))

  def same(i: Individual, j:Individual): Boolean = hermit.isSameIndividual(i, j)


  private def flat(set: NodeSet[Individual]): Array[Individual] =
    set.getFlattened().toArray(new Array[Individual](0))

  def query(c: Concept): Array[Individual] =
    flat(hermit.getInstances(c, false))

  def project(i: Individual, r: Role): Array[Individual] =
    flat(hermit.getObjectPropertyValues(i, r))


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
