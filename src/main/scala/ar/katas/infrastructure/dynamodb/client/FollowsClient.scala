package ar.katas.infrastructure.dynamodb.client

import ar.katas.domain.following.{FolloweeId, FollowerId, Following}
import ar.katas.domain.user.Nickname
import ar.katas.domain.{Follows, following}
import ar.katas.infrastructure.dynamodb.client.FollowsDynamodb._
import cats.effect.IO
import dynosaur.Schema
import meteor.api.hi.CompositeTable
import meteor.codec.Codec
import meteor.{DynamoDbType, Expression, KeyDef, Query, SortKeyQuery}
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import codecs._

object FollowsClient {
  def make(client: DynamoDbAsyncClient): Follows =
    new Follows {
      override def persistFollowing(
          idFollower: following.FollowerId,
          idFollowee: following.FolloweeId
      ): IO[Unit] =
        followsTable(client).put(
          Following(
            idFollower,
            idFollowee
          )
        )

      override def isFollowing(
          idFollower: following.FollowerId,
          idFollowee: following.FolloweeId
      ): IO[Boolean] = followsTable(client)
        .get[Following](
          partitionKey = NicknameIndex(Nickname(idFollower.value)),
          sortKey = FollowedIndex(Nickname(idFollowee.value)),
          consistentRead = true
        )
        .map(_.nonEmpty)

      override def getFollowees(
          idFollower: following.FollowerId
      ): IO[List[following.FolloweeId]] =
        followsTable(client)
          .retrieve[Following](
            Query(
              partitionKey = NicknameIndex(Nickname(idFollower.value)),
              sortKeyQuery =
                SortKeyQuery.BeginsWith(FollowedIndex(Nickname(""))),
              filter = Expression.empty
            ),
            consistentRead = true,
            limit = 2
          )
          .map(_.followeeId)
          .compile
          .toList
    }

}

private object FollowsDynamodb {
  import cats.syntax.apply._
  import codecs._
  import meteor.dynosaur.formats.conversions._
  import schemas._

  lazy val followsTable: DynamoDbAsyncClient => CompositeTable[
    IO,
    NicknameIndex,
    FollowedIndex
  ] =
    jclient =>
      CompositeTable(
        "Users",
        KeyDef[NicknameIndex]("nickname", DynamoDbType.S),
        KeyDef[FollowedIndex]("category", DynamoDbType.S),
        jclient
      )

  final case class FollowedIndex(nickname: Nickname)
  val followedIndexSchema: Schema[FollowedIndex] = Schema[String]
    .imap[FollowedIndex](s =>
      FollowedIndex(
        Nickname(
          s.split("FOLLOWED#").last
        )
      )
    )(it => s"FOLLOWED#${it.nickname.value}")

  implicit val followedCodec: Codec[FollowedIndex] = schemaToCodec(
    followedIndexSchema
  )

  val followingSchema: Schema[Following] = Schema.record(field =>
    (
      field("nickname", it => NicknameIndex(Nickname(it.followerId.value)))(
        Schema[NicknameIndex]
      ),
      field("category", it => FollowedIndex(Nickname(it.followeeId.value)))(
        Schema[FollowedIndex](followedIndexSchema)
      )
    ).mapN { case (nickname, category) =>
      Following(
        FollowerId(nickname.nickname.value),
        FolloweeId(category.nickname.value)
      )
    }
  )

  implicit val followingCodec: Codec[Following] = schemaToCodec(followingSchema)

}
