package ar.katas.actions

import ar.katas.model.following.FollowerId
import ar.katas.model.user.{Nickname, User}
import ar.katas.model.{Follows, Users}
import cats.effect.IO
import cats.syntax.traverse._

trait WhoIsFollowing {
  def exec(followerId: Nickname): IO[List[User]]
}

object WhoIsFollowing {
  def make(follows: Follows, users: Users): WhoIsFollowing =
    (followerId: Nickname) =>
      users.get(followerId) *>
        follows
          .getFollowees(FollowerId(followerId.value))
          .flatMap(followees =>
            followees.traverse(followeeId =>
              users.get(Nickname(followeeId.value))
            )
          )
}
