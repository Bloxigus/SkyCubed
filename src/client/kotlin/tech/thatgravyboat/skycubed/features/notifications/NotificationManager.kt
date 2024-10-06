package tech.thatgravyboat.skycubed.features.notifications

import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.chat.ChatReceivedEvent
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import tech.thatgravyboat.skycubed.SkyCubed
import tech.thatgravyboat.skycubed.config.notifications.NotificationsConfig

object NotificationManager {

    private val notifications = listOf(
        NotificationType.single("friendJoinLeave", "Friend > (?<name>\\w{3,16}) (?<reason>joined|left)\\.", NotificationsConfig.friendJoinLeave) { _, match ->
            joinLeaveMessage(match, "Friends", TextColor.GREEN)
        },
        NotificationType.single("guildJoinLeave", "Guild > (?<name>\\w{3,16}) (?<reason>joined|left)\\.", NotificationsConfig.guildJoinLeave) { _, match ->
            joinLeaveMessage(match, "Guild", TextColor.DARK_GREEN)
        },

        NotificationType.single("warping", "Warping\\.\\.\\.") { NotificationsConfig.warping },
        NotificationType.single("blocksInTheWay", "There are blocks in the way!") { NotificationsConfig.blocksInTheWay },

        NotificationType.unique("hoppityYouFound", "HOPPITY'S HUNT You found .*") { NotificationsConfig.hoppityYouFound },
        NotificationType.unique("hoppityEggAppeared", "HOPPITY'S HUNT A [\\w ]+ Egg has appeared!") { NotificationsConfig.hoppityEggAppeared },

        NotificationType.single("skymall1", "New buff: .*") { NotificationsConfig.skymall },
        NotificationType.unique("skymall2", "You can disable this messaging by toggling Sky Mall in your /hotm!") { NotificationsConfig.skymall.copy(showAsToast = false) },
        NotificationType.unique("skymall3", "New day! Your Sky Mall buff changed!") { NotificationsConfig.skymall.copy(showAsToast = false) }, // hide if new buff is hidden

        NotificationType.single("monolith", "MONOLITH! .*") { NotificationsConfig.monolith },

        NotificationType.single("rift_orb", "ORB! Picked up +25 Motes, recovered +2ф Rift Time!") { NotificationsConfig.riftOrb },

        NotificationType.single("combo", "\\+\\d+ Kill Combo .*|Your Kill Combo has expired!.*") { NotificationsConfig.combo },
    )

    private fun joinLeaveMessage(match: MatchGroupCollection, title: String, color: Int): Component = Text.multiline(
        Text.of(title) { this.color = color },
        Text.join(
            Text.of("${match["name"]?.value} ") { this.color = TextColor.GRAY },
            Text.of("${match["reason"]?.value}.") { this.color = if (match["reason"]?.value == "joined") TextColor.GREEN else TextColor.RED }
        )
    )

    @Subscription(priority = Subscription.HIGHEST, receiveCancelled = true)
    fun onChatMessage(event: ChatReceivedEvent) {
        val message = event.text
        for (notification in notifications) {
            val config = notification.config()
            if (!config.shouldCheck()) continue
            val match = notification.regex.matchEntire(message) ?: continue
            if (config.showAsToast) {
                NotificationToast.add(
                    notification.id,
                    notification.factory(event.component, match.groups),
                    config.toastDuration
                )
            }
            if (config.hideMessage) {
                SkyCubed.logger.info("[Cancelled] [CHAT] ${event.text}")
                event.cancel()
            }
            break
        }
    }
}