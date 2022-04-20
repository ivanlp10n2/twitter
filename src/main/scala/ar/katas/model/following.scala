package ar.katas.model

object following {

  final case class FolloweeId(value: String)

  final case class FollowerId(value: String)

  final case class Following(followerId: FollowerId, followeeId: FolloweeId)
}
