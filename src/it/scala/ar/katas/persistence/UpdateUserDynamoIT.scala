package ar.katas.persistence

import ar.katas.actions.{RegisterUser, UpdateUser}
import ar.katas.infrastructure.persistence.dynamodb.{
  DynamoDbResource,
  UsersClient
}
import ar.katas.model.user._
import munit.CatsEffectSuite

class UpdateUserDynamoIT extends CatsEffectSuite {

  test("A user can update his real name") {
    val user = User(Username("Jack Bauer"), Nickname("@jack"))
    val updatedUser = user.copy(username = Username("Jhon"))

    val client = DynamoDbResource.localDefault
    client.use(client => {
      val users = UsersClient.make(client)
      val register = RegisterUser.make(users)
      val update = UpdateUser.make(users)

      for {
        _ <- cleanUsersTable(client)
        _ <- register.exec(user)
        _ <- update.exec(updatedUser)
        actualUser <- users.get(updatedUser.nickname)
      } yield assertEquals(actualUser, updatedUser)
    })

  }
}
