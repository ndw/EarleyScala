package earleyscala

import java.util.UUID

import scala.collection.mutable

/*
 * This file has classes that are used to build a grammar.
 *
 * A grammar represents all possible strings in a language.
 * For my parser, a grammar consists of a starting rule and a set of production rules; though formally they are usually a 4 tuple, all information can be derived from my representation.
 *
 * Rules (Production rules) are simple replacements. They have a non-terminal left hand side and a set of symbols (terminal or non-terminal) on the right hand side.
 * A couple example rules could be:
 * A -> AA
 * A -> a
 * which means that A will match any number of 'a's - a, aa, aaa, etc.
 *
 * A Terminal Symbol is the final symbol of a replacement action
 * NonTerminal symbols are eventually replaced by sets of terminal symbols based on the replacement rules
 *
 * A symbol is nullable if it names at least one nullable rule
 * A nullable rule is composed only of nullable symbols, and if the rule is empty its nullable
 * Terminal symbols are not nullable
 *
 * I don't currently support the Or (|), so A -> AA | a is not usable, but it can be done by having two rules A -> AA and A -> a
 */

sealed trait Symbol {
  def repr: String //Used to pretty print the symbols. toString will have the class name too
  def nullable: Boolean
}

final case class NonTerminalSymbol(ruleName: String) extends Symbol {
  private var _nullable: Boolean = false

  override def nullable: Boolean = _nullable

  def nullable(value: Boolean): Unit = this._nullable = value

  override def repr: String = ruleName
}

case class TerminalSymbol(s: String) extends Symbol { //s should be a regex to match a single character
  private val r = s.r

  override def repr: String = s"'${r.pattern.toString}'"

  override def nullable: Boolean = false //Terminal symbols are never nullable

  def matches(input: String): Boolean = r.matches(input) //Terminal symbols can be extended to override this behavior if needed
}


case class Rule(name: String, symbols: List[Symbol], id: String = UUID.randomUUID().toString) { //duplicate rules are technically allowed, so id is to make the rules unique
  def repr: String = {
    val sb = new StringBuilder()
    sb.append(name + " -> ")
    symbols.foreach(s => sb.append(s.repr + " "))
    sb.toString
  }
}

case class Grammar(startRuleName: String, rules: List[Rule]) {
  private val nullableSymbols = new mutable.HashSet[String]()
  buildNullableSymbols()

  private def buildNullableSymbols(): Unit = {
    //FIXME: This is a O(n^2) way of finding nullable rules. It should probably be converted to a O(n) way of doing this eventually
    var s = nullableSymbols.size - 1
    while (s < nullableSymbols.size) {
      s = nullableSymbols.size
      rules.foreach(r => {
        if (r.symbols.isEmpty) {
          nullableSymbols.add(r.name)
        }
        else {
          if (r.symbols.exists(t => t.isInstanceOf[NonTerminalSymbol] && nullableSymbols.contains(t.asInstanceOf[NonTerminalSymbol].ruleName))) {
            nullableSymbols.add(r.name)
          }
        }
      })
    }
    rules.foreach(r => {
      r.symbols.foreach {
        case nt: NonTerminalSymbol => if (nullableSymbols.contains(nt.ruleName)) nt.nullable(true)
        case t: TerminalSymbol => //pass
      }
    })
  }

  def repr: String = {
    val sb = new StringBuilder()
    sb.append(s"start: $startRuleName\n")
    rules.foreach(r => sb.append(r.repr + '\n'))
    sb.toString
  }

  def getRulesByName(name: String): List[Rule] = {
    //FIXME: construct a multimap of name->rule so that getRulesByName is O(1)
    rules.filter(r => r.name == name)
  }
}
