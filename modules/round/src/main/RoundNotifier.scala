package lila.round

import lila.hub.actorApi.timeline.{ GameEnd as TLGameEnd, Propagate }
import lila.notify.{ GameEnd, Notification, NotifyApi }

import lila.game.Game
import lila.user.User

final private class RoundNotifier(
    timeline: lila.hub.actors.Timeline,
    isUserPresent: (Game, UserId) => Fu[Boolean],
    notifyApi: NotifyApi
)(using ec: scala.concurrent.ExecutionContext):

  def gameEnd(game: Game)(color: chess.Color) =
    if (!game.aborted) game.player(color).userId foreach { userId =>
      game.perfType foreach { perfType =>
        timeline ! (Propagate(
          TLGameEnd(
            fullId = game fullIdOf color,
            opponent = game.player(!color).userId,
            win = game.winnerColor map (color ==),
            perf = perfType.key.value
          )
        ) toUser userId)
      }
      isUserPresent(game, userId) foreach {
        case false =>
          notifyApi.addNotification(
            Notification.make(
              userId,
              GameEnd(
                game fullIdOf color,
                game.opponent(color).userId,
                Win from game.wonBy(color)
              )
            )
          )
        case _ =>
      }
    }
