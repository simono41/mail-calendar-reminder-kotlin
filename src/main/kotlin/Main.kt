import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.ParserException
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Calendar
import java.util.Date
import kotlin.system.exitProcess

// https://logging.apache.org/log4j/2.x/manual/configuration.html
private val logger: Logger = LogManager.getLogger("MainKt")

private var fromString = ""
private var toString = ""
private var passwordString = ""
private var hostString = ""

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    // https://www.baeldung.com/java-jvm-time-zone
    val calendar = Calendar.getInstance()
    val timeZone = calendar.timeZone
    logger.info(
        "Folgende Zeitzone ist eingestellt: " + timeZone.id + " : "
                + timeZone.getDisplayName(Locale.GERMAN)
    )

    // System.setProperty("user.timezone", "Europe/Berlin");
    // TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));

    // Use the local default timezone for the implementation of DATE properties https://www.ical4j.org/configuring/
    System.setProperty("net.fortuna.ical4j.timezone.date.floating", "true")
    val path = Paths.get("")
    val directoryName = path.toAbsolutePath().toString()
    logger.debug("Current Working Directory is = $directoryName")

    if (args.size <= 4) {
        logger.error("Es fehlen noch einige Parameter!!!")
        logger.error("ics-dir, from, to, password, host")
        exitProcess(1)
    }
    fromString = args[1]
    toString = args[2]
    passwordString = args[3]
    hostString = args[4]
    val folder = File(args[0])
    logger.info(
        "folder: " + folder.absolutePath, (" from: " + fromString
                + " to: " + toString + " password: " + passwordString + " host: " + hostString)
    )
    listFilesForFolder(folder)
}

fun listFilesForFolder(folder: File) {
    for (file: File in folder.listFiles()!!) {
        if (file.isFile) {
            logger.debug(file.absolutePath)
            getNotifications(file.absolutePath)
        }
    }
    // Beende das Programm
    exitProcess(0)
}

fun getNotifications(file: String) {
    try {
        FileInputStream(file).use { fin ->
            val builder = CalendarBuilder()
            val calendar: net.fortuna.ical4j.model.Calendar = builder.build(fin)
            var isValidVEVENT = false

            for (o: Component in calendar.components) {
                logger.debug("Component: " + o.name)
                for (o1: Property in o.properties) {
                    logger.debug(o1.name + ": " + o1.value)
                }

                if (o.name.equals("VEVENT"))
                    isValidVEVENT = true
            }

            if (isValidVEVENT) {
                // http://www.cheerfulprogramming.com/blog/2021/03/09/intro-to-ical4j
                val event: VEvent = calendar.getComponent(Component.VEVENT)

                // https://www.ical4j.org/examples/recur/
                // https://stackoverflow.com/questions/1005523/how-to-add-one-day-to-a-date
                // Create the date range which is desired.
                val dt: Date = Date()
                val arrival: Calendar = Calendar.getInstance()
                arrival.time = dt
                arrival.set(Calendar.SECOND, 1)
                arrival.set(Calendar.MINUTE, 0)
                arrival.set(Calendar.HOUR, 0)
                val departure: Calendar = Calendar.getInstance()
                departure.time = dt
                departure.set(Calendar.SECOND, 59)
                departure.set(Calendar.MINUTE, 59)
                departure.set(Calendar.HOUR, 23)
                // departure.add(Calendar.DATE, 1);
                val fromTime: DateTime = DateTime(arrival.time)
                val toTime: DateTime = DateTime(departure.time)
                val period: Period = Period(fromTime, toTime)
                val list: PeriodList = event.calculateRecurrenceSet(period)
                for (po: Period in list) {
                    val component: Component = po.component
                    val summary: Summary =
                        component.getProperty(Property.SUMMARY)
                    val dtStart: DtStart = component.getProperty(Property.DTSTART)
                    val dtEnd: DtEnd = component.getProperty(Property.DTEND)
                    val location: Location? =
                        component.getProperty(Property.LOCATION)
                    val description: Description? =
                        component.getProperty(Property.DESCRIPTION)

                    // Summary summary = po.getSummary();
                    val messageSubject: String = ("Es existiert für heute ein neuer Kalendereintrag Namens: "
                            + summary.value)
                    logger.info(messageSubject)
                    var messageText: String = ("Der Termin beginnt heute um: " + formatDateTime(dtStart.date)
                            + " und endet um: " + formatDateTime(dtEnd.date) + ".")
                    if (location != null) {
                        messageText += "\n\n Er findet in " + location.value + " statt."
                    }
                    if (description != null) {
                        messageText += "\n\n Folgende Notiz existiert in diesen Eintrag: \n" + description.value
                    }
                    messageText += "\n\n This email is a service from mail-calendar-reminder Version $VERSION. \n Delivered by Simon Rieger"
                    logger.info(messageText)
                    sendMail(messageSubject, messageText)
                }
            }
        }
    } catch (e: IOException) {
        logger.error(e)
    } catch (e: ParserException) {
        logger.error(e)
    }
}

private fun formatDateTime(date: Date): String {
    // In
    // DateTimeFormatter inFormatter =
    // DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    // LocalDateTime dateTime = LocalDateTime.parse(date, inFormatter);
    val dateTime = convertToLocalDateTimeViaMilisecond(date)

    // Out
    val outFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return dateTime.format(outFormatter)
}

// https://www.baeldung.com/java-date-to-localdate-and-localdatetime
fun convertToLocalDateTimeViaMilisecond(dateToConvert: Date): LocalDateTime {
    return Instant.ofEpochMilli(dateToConvert.time)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

private fun sendMail(messageSubject: String, messageText: String) {
    // configure Mailtrap's SMTP server details
    // https://stackoverflow.com/questions/43511080/mailconnectexception-couldnt-connect-to-host-port-smtp-gmail-com-465-timeo
    val props = Properties()
    props["mail.smtp.auth"] = "true"
    props["mail.smtp.host"] = hostString
    props["mail.smtp.port"] = "465"
    props["mail.transport.protocol"] = "smtp"
    props["mail.smtp.ssl.enable"] = "true"
    props["mail.debug"] = "true"

    // create the Session object
    val authenticator: Authenticator = object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(fromString, passwordString)
        }
    }
    val session = Session.getInstance(props, authenticator)
    try {
        // create a MimeMessage object
        val message: Message = MimeMessage(session)
        // set From email field
        message.setFrom(InternetAddress(fromString))
        // set To email field
        message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(toString)
        )
        // set email subject field
        message.subject = messageSubject
        // set the content of the email message
        message.setText(messageText)
        // send the email message
        Transport.send(message)
        logger.info("Email Message Sent Successfully")
    } catch (e: MessagingException) {
        logger.error(e)
    }
}