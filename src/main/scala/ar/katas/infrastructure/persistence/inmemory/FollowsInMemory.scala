package ar.katas.infrastructure.persistence.inmemory

import ar.katas.model.Follows
import ar.katas.model.following.{FolloweeId, FollowerId}
import cats.effect.IO

object FollowsInMemory {
  val make: IO[Follows] =
    IO.ref(Map.empty[FollowerId, List[FolloweeId]])
      .map(database => {
        new Follows {
          override def persistFollowing(
              idFollower: FollowerId,
              idFollowee: FolloweeId
          ): IO[Unit] =
            database.update { m =>
              val updatedList =
                idFollowee :: m.getOrElse(idFollower, List.empty)
              m + (idFollower -> updatedList)
            }

          override def isFollowing(
              idFollower: FollowerId,
              idFollowee: FolloweeId
          ): IO[Boolean] =
            database.get.map(m =>
              m
                .get(idFollower)
                .exists(l => l.contains(idFollowee))
            )

          override def getFollowees(
              idFollower: FollowerId
          ): IO[List[FolloweeId]] =
            database.get
              .map(m => m.getOrElse(idFollower, List.empty[FolloweeId]))
        }
      })

}
