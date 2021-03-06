package ar.katas.infrastructure.persistence.inmemory

import ar.katas.model.Users
import ar.katas.model.user.{Nickname, User, UserNotFound, Username}
import cats.effect.IO
import cats.implicits.catsSyntaxOption

object UsersInMemory {
  val make: IO[Users] = {
    IO.ref(Map.empty[Nickname, Username])
      .map(database => {
        new Users {
          override def persist(user: User): IO[Unit] =
            database.update(m => m + (user.nickname -> user.username))

          override def exists(id: Nickname): IO[Boolean] =
            database.get.map(m => m.contains(id))

          override def get(nickname: Nickname): IO[User] =
            database.get.flatMap(m =>
              m.collectFirst {
                case (nick, name) if nick == nickname =>
                  User(name, nick)
              }.liftTo[IO](UserNotFound(nickname))
            )

          override def update(user: User): IO[Unit] =
            exists(user.nickname).ifM(
              ifTrue =
                database.update(m => m + (user.nickname -> user.username)),
              ifFalse = IO.raiseError(UserNotFound(user.nickname))
            )
        }
      })
  }
}
