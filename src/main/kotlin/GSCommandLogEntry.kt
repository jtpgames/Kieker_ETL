import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class GSCommandLogEntry(
    val id: Long,
    val timestamp: Instant,
    val startOrEndOfCmd: String,
    val operation: String
)
{
    override fun toString(): String
    {
        // operation format: ID_<command name>
        // Example: ID_REQ_LCMD_MONGETMASTERSTATUS

        // convert to the GS command log format
        return "[$id]".padEnd(13) +
                " ${formatter.format(timestamp)} " +
                " ${startOrEndOfCmd.padEnd(9)} " +
                " ID_$operation ";
    }

    companion object
    {
        private val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withLocale(Locale.GERMANY)
            .withZone(ZoneId.systemDefault())
    }
}
