package retier
package impl
package engine.generators

import engine._
import scala.reflect.macros.blackbox.Context

trait PeerImplementationGenerator { this: Generation =>
  val c: Context
  import c.universe._

  val generatePeerImplementations = UniformAggregation[
    EnclosingContext with PeerDefinition with
    PlacedStatement with PlacedAbstraction with
    PeerConnectionMultiplicity] {
      aggregator =>

    echo(verbose = true, " Generating peer implementations")

    val synthetic = Flag.SYNTHETIC
    val peerSymbols = aggregator.all[PeerDefinition] map { _.peerSymbol }
    val enclosingName = aggregator.all[EnclosingContext].head.name

    class PlacedReferenceAdapter(peerSymbol: TypeSymbol) extends Transformer {
      val baseClasses = peerSymbol.toType.baseClasses

      def placedType(tpe: Type): Option[Type] =
        if (tpe != null && tpe.finalResultType <:< types.localOn &&
            (types.bottom forall { tpe.finalResultType <:!< _ }))
          Some(tpe.finalResultType)
        else
          None

      def placedType(tree: Tree): Option[Type] =
        placedType(tree.tpe) orElse {
          if (tree.symbol.isTerm && tree.symbol.asTerm.isAccessor)
            placedType(tree.symbol.asMethod.accessed.typeSignature)
          else
            None
        }

      override def transform(tree: Tree) = tree match {
        case tree: TypeTree if tree.original != null =>
          internal setOriginal (tree, transform(tree.original))

        case q"$_.$name" if !tree.isRetierSynthetic =>
          placedType(tree) match {
            case Some(tpe) =>
              val Seq(_, peerType) = tpe.widen.typeArgs
              if (baseClasses contains peerType.typeSymbol)
                q"$name"
              else
                super.transform(tree)
            case _ =>
              super.transform(tree)
          }

        case _ =>
          super.transform(tree)
      }
    }

    def createDeclTypeTree(declTypeTree: Tree, exprType: Type) =
      if (types.bottom exists { exprType <:< _ })
        declTypeTree
      else if (types.controlledIssuedPlacing exists { exprType <:< _ }) {
        val Seq(peer, value) = declTypeTree.typeArgTrees
        tq"$peer => $value"
      }
      else if (types.issuedPlacing exists { exprType <:< _ }) {
        val Seq(_, value) = declTypeTree.typeArgTrees
        value
      }
      else
        declTypeTree

    def flagDefinition(mods: Modifiers, expr: Tree) =
      Modifiers(
        if (expr == EmptyTree) mods.flags | Flag.DEFERRED else mods.flags,
        mods.privateWithin, mods.annotations)

    def peerPlacedAbstractions(peerSymbol: TypeSymbol) =
      aggregator.all[PlacedAbstraction] filter {
        _.peerSymbol == peerSymbol
      }

    def peerConnectionMultiplicities(peerSymbol: TypeSymbol) =
      aggregator.all[PeerConnectionMultiplicity] filter {
        _.peerSymbol == peerSymbol
      }

    def peerPlacedStatements(peerSymbol: TypeSymbol) =
      aggregator.all[PlacedStatement] collect {
        case PlacedStatement(
            definition @ ValDef(mods, name, _, _),
            `peerSymbol`, exprType, Some(declTypeTree), _, expr) =>
          internal setPos (
            new PlacedReferenceAdapter(peerSymbol) transform
              ValDef(flagDefinition(mods, expr), name,
                createDeclTypeTree(declTypeTree, exprType), expr),
            definition.pos)

        case PlacedStatement(
            definition @ DefDef(mods, name, tparams, vparamss, _, _),
            `peerSymbol`, exprType, Some(declTypeTree), _, expr) =>
          internal setPos (
            new PlacedReferenceAdapter(peerSymbol) transform
              DefDef(flagDefinition(mods, expr), name, tparams, vparamss,
                createDeclTypeTree(declTypeTree, exprType), expr),
            definition.pos)

        case PlacedStatement(tree, `peerSymbol`, _, None, _, expr) =>
          new PlacedReferenceAdapter(peerSymbol) transform
            expr
      }

    def peerImplementationParents(parents: List[Tree]) = parents map {
      case parent if parent.tpe =:= types.peer =>
        trees.PeerImpl

      case parent @ tq"$_[..$tpts]" if parent.tpe <:< types.peer =>
        val impl = peerImplementationTree(parent, parent.tpe, peerSymbols)
        tq"$impl[..$tpts]"

      case parent =>
        parent
    }

    def processPeerCompanion(peerDefinition: PeerDefinition) = {
      val PeerDefinition(tree, peerSymbol, typeArgs, args, parents, mods,
        _, isClass, companion) = peerDefinition

      val peerName = peerSymbol.name
      val abstractions = peerPlacedAbstractions(peerSymbol)
      val statements = peerPlacedStatements(peerSymbol)
      val implParents = peerImplementationParents(parents)

      val duplicateName = peerSymbol.toType.baseClasses filter {
        _.asType.toType <:< types.peer
      } groupBy {
        _.name
      } collectFirst {
        case (name, symbols) if symbols.size > 1 => name
      }

      if (duplicateName.nonEmpty)
        c.abort(tree.pos,
          s"inheritance from peer types of the same name " +
          s"is not allowed: ${duplicateName.get}")

      import trees._
      import names._

      val syntheticMods = Modifiers(
        mods.flags | Flag.SYNTHETIC, mods.privateWithin, mods.annotations)

      val dispatchImpl =
        q"""$synthetic override def $dispatch(
                request: $String,
                id: $AbstractionId,
                ref: $AbstractionRef): $Try[$String] = {
              import _root_.retier.impl.AbstractionRef._
              import _root_.retier.impl.RemoteRef._
              id match {
                case ..${abstractions map { _.dispatchClause } }
                case _ => super.$dispatch(request, id, ref)
              }
            }
         """

      val systemImpl = q"$synthetic override lazy val $system = new $System"

      val peerImpl =
        if (isClass)
          q"""$syntheticMods class $implementation[..$typeArgs](...$args)
                  extends ..$implParents {
                $systemImpl
                $dispatchImpl
                ..$statements
          }"""
        else
          q"""$syntheticMods trait $implementation[..$typeArgs]
                  extends ..$implParents {
                $systemImpl
                $dispatchImpl
                ..$statements
          }"""

      val peerInterface =
        q"""$synthetic object $interface {
              ..${abstractions flatMap { _.interfaceDefinitions } }
        }"""

      val generatedCompanion = companion match {
        case Some(q"""$mods object $tname
                    extends { ..$earlydefns } with ..$parents { $self =>
                    ..$stats
                  }""") =>
          q"""$mods object $tname
                  extends { ..$earlydefns } with ..$parents { $self =>
                ${markRetierSynthetic(peerInterface)}
                ${markRetierSynthetic(peerImpl)}
                ..$stats
          }"""

        case _ =>
          markRetierSynthetic(
            q"""$synthetic object ${peerName.toTermName} {
                  $peerInterface
                  $peerImpl
            }""")
      }

      peerDefinition.copy(companion = Some(generatedCompanion))
    }

    def processPeerDefinition(peerDefinition: PeerDefinition) = {
      val PeerDefinition(tree, peerSymbol, typeArgs, args, parents, mods, stats,
        isClass, _) = peerDefinition

      val peerName = peerSymbol.name
      val multiplicities = peerConnectionMultiplicities(peerSymbol)

      import trees._
      import names._

      stats foreach {
        case stat: ValOrDefDef if stat.name == connection =>
          c.abort(stat.pos,
            s"member of name `$connection` not allowed in peer definitions")
        case _ =>
      }

      val hasPeerParents = parents exists { parent =>
        parent.tpe <:< types.peer && parent.tpe =:!= types.peer
      }

      val peerMultiplicities = multiplicities map {
        case PeerConnectionMultiplicity( _, connectedPeer,
            connectionMultiplicity) =>
          q"($peerTypeOf[$connectedPeer], $connectionMultiplicity)"
      }

      val peerConnections =
        if (peerMultiplicities.nonEmpty || !hasPeerParents)
          q"$Map[$PeerType, $ConnectionMultiplicity](..$peerMultiplicities)"
        else
          q"super.$connection"

      val connectionImpl = q"$synthetic def $connection = $peerConnections"

      val generatedStats = markRetierSynthetic(connectionImpl) :: stats
      val generatedTree =
        if (isClass)
          q"""$mods class $peerName[..$typeArgs](...$args)
                  extends ..$parents {
                ..$generatedStats
          }"""
        else
          q"""$mods trait $peerName[..$typeArgs]
                  extends ..$parents {
                ..$generatedStats
          }"""

      peerDefinition.copy(
        tree = internal setPos (internal setType (
          generatedTree, tree.tpe), tree.pos),
        stats = generatedStats)
    }

    val definitions = aggregator.all[PeerDefinition] map
      (processPeerCompanion _ compose processPeerDefinition _)

    echo(verbose = true,
      s"  [${definitions.size} peer definitions generated, existing replaced]")

    aggregator replace definitions
  }
}
